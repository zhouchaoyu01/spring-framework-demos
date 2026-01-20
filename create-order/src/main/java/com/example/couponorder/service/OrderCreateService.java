package com.example.couponorder.service;


import com.alibaba.fastjson2.JSON;
import com.example.couponorder.client.*;
import com.example.couponorder.entity.OrderCouponVerifyDO;
import com.example.couponorder.entity.OrderDO;
import com.example.couponorder.exception.BusinessException;
import com.example.couponorder.mapper.OrderCouponVerifyMapper;
import com.example.couponorder.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class OrderCreateService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderCouponVerifyMapper verifyMapper;
    @Autowired
    private CouponClient couponClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate; // 用于编排细粒度事务

    // 常量定义
    private static final String IDEMPOTENT_PREFIX = "order:req:";
    private static final String WAL_PREFIX = "sys:order:flow:";
    /**
     * 创建订单请求
     */
    public static class OrderCreateReq {
        private String requestId;
        private Long userId;
        private String couponId;
        private String productId;
        private Integer quantity;
        private Long amount;

        // Getters and Setters
        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getCouponId() {
            return couponId;
        }

        public void setCouponId(String couponId) {
            this.couponId = couponId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }
    }

    /**
     * 创建订单核心逻辑 (优化版)
     */
    public String createOrder(OrderCreateReq req) throws Exception {
        String requestId = req.getRequestId();
        String userId = String.valueOf(req.getUserId());
        String orderId = UUID.randomUUID().toString(); // 建议生产环境换成雪花算法

        // -------------------------------------------------------
        // 1. [性能与幂等] Redis 原子防重 (Fail-Fast，无锁设计)
        // -------------------------------------------------------
        // SET key value NX EX 600
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENT_PREFIX + requestId, "PROCESSING", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isFirst)) {
            // 快速失败，不阻塞线程
            throw new BusinessException("请勿重复提交，您的请求正在处理中");
        }

        try {
            // -------------------------------------------------------
            // 2. [数据安全] WAL 预写日志 (关键：防 JVM 宕机导致券丢失)
            // -------------------------------------------------------
            // 如果后续步骤宕机，后台 Job 扫描此 Key 发现无 DB 记录，会自动触发冲正
            String walKey = WAL_PREFIX + orderId;
            redisTemplate.opsForValue().set(walKey, JSON.toJSONString(req), Duration.ofMinutes(30));

            boolean couponUsed = false;
            String extMsg = "";

            // -------------------------------------------------------
            // 3. [外部调用] 核销优惠券 (绝不要放在 DB 事务内！)
            // -------------------------------------------------------
            if (StringUtils.hasText(req.getCouponId())) {
                try {
                    CouponVerifyReq verifyReq = new CouponVerifyReq();
                    verifyReq.setCouponId(req.getCouponId());
                    verifyReq.setOrderId(orderId);
                    verifyReq.setUserId(req.getUserId());

                    log.info("开始调用优惠券核销: {}", orderId);
                    CouponVerifyResp resp = couponClient.verifyCoupon(verifyReq);

                    if (!resp.isSuccess()) {
                        // 明确的业务失败，可以安全地抛出异常并允许重试
                        throw new BusinessException("优惠券无效: " + resp.getMsg());
                    }
                    couponUsed = true;
                    extMsg = resp.getMsg();

                } catch (Exception e) {
                    handleCouponException(e, requestId, walKey);
                }
            }

            // -------------------------------------------------------
            // 4. [落库] 极短事务 (只包含 DB 操作)
            // -------------------------------------------------------
            final boolean finalCouponUsed = couponUsed;
            final String finalExtMsg = extMsg;

            transactionTemplate.execute(status -> {
                try {
                    // 4.1 插入订单 (直接 CREATED，无需 Insert -> Update)
                    OrderDO order = new OrderDO();
                    order.setOrderId(orderId);
                    order.setUserId(req.getUserId());
                    order.setCouponId(req.getCouponId());
                    order.setStatus("CREATED");
                    order.setCreateTime(new Date());
                    order.setUpdateTime(new Date());
                    orderMapper.insert(order);

                    // 4.2 插入核销记录 (如果用了券)
                    if (finalCouponUsed) {
                        OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
                        verifyDO.setOrderId(orderId);
                        verifyDO.setCouponId(req.getCouponId());
                        verifyDO.setVerifyStatus("SUCCESS");
                        verifyDO.setVerifyTime(new Date());
                        verifyDO.setExtMsg(finalExtMsg);
                        verifyDO.setUniqueKey(req.getCouponId() + "_" + orderId); // 唯一索引防重
                        verifyDO.setCreateTime(new Date());
                        verifyMapper.insert(verifyDO);
                    }
                    return Boolean.TRUE;
                } catch (DuplicateKeyException e) {
                    // 极低概率：UUID 碰撞或数据库层面拦截了重复请求
                    log.warn("订单落库主键冲突: {}", orderId);
                    status.setRollbackOnly();
                    throw new BusinessException("订单已存在");
                } catch (Exception e) {
                    // DB 异常回滚
                    status.setRollbackOnly();
                    throw e;
                }
            });

            // -------------------------------------------------------
            // 5. [清理] 流程成功，删除 WAL 日志
            // -------------------------------------------------------
            // 此时订单已在库，后续不再需要 Job 兜底，保留 IDEMPOTENT Key 防止重复提交
            redisTemplate.delete(walKey);

        } catch (Exception e) {
            log.error("下单流程异常 orderId={}", orderId, e);
            // 触发尽力而为的补偿（针对 Step 4 DB 失败但 Step 3 券已扣的情况）
            if (StringUtils.hasText(req.getCouponId())) {
                asyncReverseCoupon(req.getCouponId(), req.getUserId(), orderId);
            }
            throw e; // 继续向上抛出
        }

        return orderId;
    }

    /**
     * 专门处理优惠券调用异常
     */
    private void handleCouponException(Exception e, String requestId, String walKey) throws Exception {
        if (e instanceof SocketTimeoutException) {
            // 【关键】超时异常（薛定谔状态）：不能删防重 Key，不能删 WAL
            // 让用户稍后查询，或者等待后台 Job 最终确认
            log.warn("优惠券核销超时，状态未知");
            throw new BusinessException("系统繁忙，正在确认核销结果，请勿重复下单");
        } else if (e instanceof BusinessException) {
            // 明确的业务失败（如券过期）：清理 Redis，允许用户换张券重试
            redisTemplate.delete(IDEMPOTENT_PREFIX + requestId);
            redisTemplate.delete(walKey);
            throw e;
        } else {
            // 其他网络错误（Connection Refused）：视为未核销，允许重试
            redisTemplate.delete(IDEMPOTENT_PREFIX + requestId);
            redisTemplate.delete(walKey);
            throw new BusinessException("优惠券服务暂时不可用: " + e.getMessage());
        }
    }

    // ... 其他依赖注入

    // 注入专门用于"旁路逻辑"的线程池，避免占用 Tomcat 主线程
    @Autowired
    @Qualifier("compensationExecutor")
    private Executor compensationExecutor;

    /**
     * 异步冲正逻辑 (Best Effort 尽力而为模式)
     *
     * 设计思路：
     * 1. 这是一个"非关键路径"，不能阻塞下单主流程。
     * 2. 如果这里执行成功，顺手把 Redis 里的 WAL 日志删了，帮兜底 Job 减轻负担。
     * 3. 如果这里执行失败（线程池满、网络超时），不做重试，完全交给兜底 Job 处理。
     */
    private void asyncReverseCoupon(String couponId, Long userId, String orderId) {
        try {
            // 提交到独立线程池执行
            compensationExecutor.execute(() -> {
                String logPrefix = "[异步冲正-" + orderId + "] ";
                try {
                    log.info(logPrefix + "开始执行");

                    // 1. 组装请求
                    CouponRollbackReq req = new CouponRollbackReq();
                    req.setCouponId(couponId);
                    req.setOrderId(orderId);
                    req.setReason("下单异常自动回滚");

                    // 2. 调用外部接口 (RPC)
                    CouponRollbackResp resp = couponClient.rollbackCoupon(req);

                    // 3. 处理结果
                    if (resp != null && resp.isSuccess()) {
                        log.info(logPrefix + "执行成功，清理 WAL 日志");
                        // 【关键优化】
                        // 既然已经冲正成功了，就删除 Step 2 留下的 WAL Key。
                        // 这样兜底 Job 就不需要再扫描处理这条数据了，减少系统开销。
                        redisTemplate.delete(WAL_PREFIX + orderId);
                        // 同时清理防重 Token (视业务策略而定，通常建议保留防重Token防止用户立刻重试)
                        // redisTemplate.delete(IDEMPOTENT_PREFIX + req.getRequestId());
                    } else {
                        log.warn(logPrefix + "外部系统返回失败: {}", resp != null ? resp.getMsg() : "null");
                        // 这里不需要抛异常，也不需要重试，留给 Job 稍后处理
                    }
                } catch (Exception e) {
                    log.error(logPrefix + "执行异常，将等待 Job 兜底", e);
                    // 吞掉异常，确保线程安全退出
                }
            });
        } catch (Exception e) {
            // 捕获任务提交阶段的异常（例如线程池队列满了拒绝策略抛出的异常）
            log.error("[异步冲正] 任务提交失败，将等待 Job 兜底: orderId={}", orderId, e);
        }
    }

    // OrderCreateReq 内部类保持不变...
}