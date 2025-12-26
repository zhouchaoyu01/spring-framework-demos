package com.example.couponorder.service;

import com.example.couponorder.client.CouponClient;
import com.example.couponorder.client.CouponVerifyReq;
import com.example.couponorder.client.CouponVerifyResp;
import com.example.couponorder.entity.OrderDO;
import com.example.couponorder.entity.OrderCouponVerifyDO;
import com.example.couponorder.exception.BusinessException;
import com.example.couponorder.mapper.OrderMapper;
import com.example.couponorder.mapper.OrderCouponVerifyMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/**
 * 订单创建服务
 */
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
    private RepeatSubmitGuard repeatSubmitGuard;
    @Autowired
    private RedissonClient redissonClient;
    private Integer requestNum = 0;
    private Integer requestS = 0;
    private Integer requestF = 0;
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
     * 创建订单核心逻辑（无分布式事务+无订单中间状态）
     */
    public String createOrder(OrderCreateReq req) {

        // 1 生成订单ID
        String orderId = UUID.randomUUID().toString();
        requestNum++;
        String lockName = "order:create:" + req.getUserId();
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 2. 尝试加锁：等待3秒，持有10秒，超时自动释放
            boolean locked = lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS);
            if (locked) {
                requestS++;
                // 1. 防重校验（RequestId+数据库唯一索引）
                if (!repeatSubmitGuard.validateRequestId(req.getRequestId(), req.getUserId())) {
                    throw new BusinessException("重复请求，请稍后再试");
                }


                String couponId = req.getCouponId();

                // 3. 本地事务：创建订单（初始状态FAILED）+ 写入核销记录（INIT）
                try {
                    initOrderAndVerifyRecord(orderId, req, couponId);
                } catch (Exception e) {
                    log.error("订单{}初始化失败", orderId, e);
                    throw new BusinessException("订单创建失败");
                }

                // 4. 如果没有优惠券，直接更新订单为CREATED
                if (couponId == null || couponId.isEmpty()) {
                    try {
                        updateOrderToCreated(orderId);
                        return orderId;
                    } catch (Exception e) {
                        log.error("订单{}更新为CREATED失败", orderId, e);
                        throw new BusinessException("订单创建中，请稍后刷新");
                    }
                }

                // 5. 调用优惠券核销接口（外部HTTP，无事务包裹）
                boolean verifySuccess = false;
                String extMsg = "";
                try {
                    CouponVerifyReq verifyReq = new CouponVerifyReq();
                    verifyReq.setCouponId(couponId);
                    verifyReq.setOrderId(orderId);
                    verifyReq.setUserId(req.getUserId());
                    log.info("订单{}调用优惠券核销接口：{}", orderId, couponId);
                    CouponVerifyResp resp = couponClient.verifyCoupon(verifyReq);
                    verifySuccess = resp.isSuccess();
                    extMsg = resp.getMsg();
                    log.info("订单{}优惠券核销结果：{}", orderId, verifySuccess);
                } catch (Exception e) {
                    log.error("订单{}核销优惠券失败", orderId, e);
                    extMsg = "核销接口调用异常：" + e.getMessage();
                    // 6. 核销失败：更新核销记录为FAILED，订单保持FAILED
                    updateVerifyRecord(orderId, couponId, "FAILED", extMsg);
                    throw new BusinessException("优惠券核销失败");
                }

                // 7. 核销成功：更新核销记录为SUCCESS + 订单改为CREATED
                if (verifySuccess) {
                    try {
                        updateOrderAndVerifySuccess(orderId, couponId, extMsg);
                    } catch (Exception e) {
                        log.error("订单{}核销成功但更新状态失败，需补偿", orderId, e);
                        // 无需立即处理，定时任务兜底，返回"订单创建中"
                        throw new BusinessException("订单创建中，请稍后刷新");
                    }
                }else {
                    throw new BusinessException("优惠券："+couponId+" 核销失败，已经被核销");
                }
            } else {
                requestF++ ;
                log.error("获取锁失败，跳过本次补偿");
                throw new BusinessException("获取锁失败，跳过本次补偿");
            }
        }catch (Exception e){
            log.error("订单创建失败", e);
            throw new BusinessException("订单创建失败");
        }finally {
            log.info("请求总数：{}，获取到锁数：{}，获取失败数：{}", requestNum, requestS, requestF);
            // 4. 释放锁（必须在finally中，避免死锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return orderId;
    }

    /**
     * 本地事务：初始化订单（FAILED）+ 核销记录（INIT）
     */
    @Transactional(rollbackFor = Exception.class)
    public void initOrderAndVerifyRecord(String orderId, OrderCreateReq req, String couponId) {
        // 创建订单（初始状态FAILED）
        OrderDO order = new OrderDO();
        order.setOrderId(orderId);
        order.setUserId(req.getUserId());
        order.setCouponId(couponId);
        order.setStatus("FAILED"); // 初始状态为失败，核销成功后再改
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderMapper.insert(order);

        // 如果有优惠券，写入核销记录（INIT）
        if (couponId != null && !couponId.isEmpty()) {
            OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
            verifyDO.setOrderId(orderId);
            verifyDO.setCouponId(couponId);
            verifyDO.setVerifyStatus("INIT");
            verifyDO.setUniqueKey(couponId + "_" + orderId); // 防重复核销
            verifyDO.setCreateTime(new Date());
            verifyDO.setUpdateTime(new Date());
            verifyMapper.insert(verifyDO);
        }
    }

    /**
     * 本地事务：更新订单为CREATED
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderToCreated(String orderId) {
        OrderDO order = new OrderDO();
        order.setOrderId(orderId);
        order.setStatus("CREATED");
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);
    }

    /**
     * 本地事务：更新订单为CREATED + 核销记录为SUCCESS
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderAndVerifySuccess(String orderId, String couponId, String extMsg) {
        // 更新订单状态为CREATED
        OrderDO order = new OrderDO();
        order.setOrderId(orderId);
        order.setStatus("CREATED");
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        // 更新核销记录为SUCCESS
        OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
        verifyDO.setOrderId(orderId);
        verifyDO.setCouponId(couponId);
        verifyDO.setVerifyStatus("SUCCESS");
        verifyDO.setVerifyTime(new Date());
        verifyDO.setExtMsg(extMsg);
        verifyDO.setUpdateTime(new Date());
        verifyMapper.updateByOrderIdAndCouponId(verifyDO);
    }

    /**
     * 本地事务：更新核销记录状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVerifyRecord(String orderId, String couponId, String status, String extMsg) {
        OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
        verifyDO.setOrderId(orderId);
        verifyDO.setCouponId(couponId);
        verifyDO.setVerifyStatus(status);
        verifyDO.setExtMsg(extMsg);
        verifyDO.setUpdateTime(new Date());
        verifyMapper.updateByOrderIdAndCouponId(verifyDO);
    }
}