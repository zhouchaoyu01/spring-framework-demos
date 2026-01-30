package com.example.coupon.service;

import com.example.coupon.dto.CouponVerifyRequest;
import com.example.coupon.dto.CouponVerifyResponse;
import com.example.coupon.entity.CouponDO;
import com.example.coupon.entity.CouponRequestRecordDO;
import com.example.coupon.entity.CouponUsageRecordDO;
import com.example.coupon.exception.CouponException;
import com.example.coupon.mapper.CouponMapper;
import com.example.coupon.mapper.CouponRequestRecordMapper;
import com.example.coupon.mapper.CouponUsageRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * 优惠券核销服务
 */
@Slf4j
@Service
public class CouponVerifyService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponUsageRecordMapper usageRecordMapper;

    @Autowired
    private CouponRequestRecordMapper requestRecordMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENT_PREFIX = "coupon:req:";

    /**
     * 核销优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public CouponVerifyResponse verifyCoupon(CouponVerifyRequest request) {

        // SET key value NX EX 600
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(IDEMPOTENT_PREFIX + request.getRequestId(), "PROCESSING", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isFirst)) {
            // 快速失败，不阻塞线程
            throw new CouponException("请勿重复提交，您的请求正在处理中");
        }

        String lockName = "coupon_verify_lock:" + request.getCouponId();
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 2. 尝试加锁：等待3秒，持有10秒，超时自动释放
            boolean locked = lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS);
            if (locked) {

                // 1. 防重校验
//                if (!validateRequest(request.getRequestId(), request.getCouponId(), request.getOrderId(), "VERIFY")) {
//                    log.warn("重复的核销请求：couponId={}, orderId={}, requestId={}",
//                            request.getCouponId(), request.getOrderId(), request.getRequestId());
//                    throw new CouponException("重复的核销请求", "DUPLICATE_REQUEST");
//                }

                // 2. 查询优惠券
                CouponDO coupon = couponMapper.selectByCouponId(request.getCouponId());
                if (coupon == null) {
                    log.warn("优惠券不存在：couponId={}", request.getCouponId());
                    throw new CouponException("优惠券不存在", "COUPON_NOT_FOUND");
                }

                // 3. 验证优惠券状态
                validateCouponStatus(coupon, request.getUserId());

                // 4. 检查是否已为该订单核销过
                CouponUsageRecordDO existingRecord = usageRecordMapper.selectVerifyRecord(request.getCouponId(), request.getOrderId());
                if (existingRecord != null) {
                    log.warn("优惠券已为该订单核销：couponId={}, orderId={}", request.getCouponId(), request.getOrderId());
                    // 返回成功，实现幂等
                    CouponVerifyResponse response = new CouponVerifyResponse();
                    response.setSuccess(true);
                    response.setMsg("优惠券已核销");
                    response.setVerifyId(existingRecord.getRecordId());
                    response.setCouponId(request.getCouponId());
                    response.setOrderId(request.getOrderId());
                    return response;
                }

                // 5. 更新优惠券状态为已使用
                int updateCount = couponMapper.updateStatus(request.getCouponId(), "USED", new Date());
                if (updateCount == 0) {
                    log.warn("优惠券状态更新失败，可能已被其他线程处理：couponId={}", request.getCouponId());
                    throw new CouponException("优惠券状态已变更，请重新尝试", "COUPON_STATUS_CHANGED");
                }

                // 6. 记录使用记录
                CouponUsageRecordDO usageRecord = new CouponUsageRecordDO();
                usageRecord.setRecordId(UUID.randomUUID().toString());
                usageRecord.setCouponId(request.getCouponId());
                usageRecord.setUserId(request.getUserId());
                usageRecord.setOrderId(request.getOrderId());
                usageRecord.setUsageType("VERIFY");
                usageRecord.setUsageTime(new Date());
                usageRecord.setRemark("订单核销：" + request.getOrderId());
                usageRecord.setCreateTime(new Date());
                usageRecordMapper.insert(usageRecord);

                log.info("优惠券核销成功：couponId={}, orderId={}, userId={}",
                        request.getCouponId(), request.getOrderId(), request.getUserId());

                // 7. 返回成功响应
                CouponVerifyResponse response = new CouponVerifyResponse();
                response.setSuccess(true);
                response.setMsg("优惠券核销成功");
                response.setVerifyId(usageRecord.getRecordId());
                response.setCouponId(request.getCouponId());
                response.setOrderId(request.getOrderId());
                return response;
            } else {
                log.error("获取锁失败，跳过本次补偿");
            }
        }catch (Exception e){
            log.error("优惠券核销异常：couponId={}, orderId={}", request.getCouponId(), request.getOrderId(), e);
            throw new CouponException("优惠券核销异常", e);
        }finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return null;

    }

    /**
     * 冲正优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public CouponVerifyResponse rollbackCoupon(CouponVerifyRequest request) {
        // 1. 防重校验
        if (!validateRequest(request.getRequestId(), request.getCouponId(), request.getOrderId(), "ROLLBACK")) {
            log.warn("重复的冲正请求：couponId={}, orderId={}, requestId={}", 
                     request.getCouponId(), request.getOrderId(), request.getRequestId());
            throw new CouponException("重复的冲正请求", "DUPLICATE_REQUEST");
        }

        // 2. 查询优惠券
        CouponDO coupon = couponMapper.selectByCouponId(request.getCouponId());
        if (coupon == null) {
            log.warn("优惠券不存在：couponId={}", request.getCouponId());
            throw new CouponException("优惠券不存在", "COUPON_NOT_FOUND");
        }

        // 3. 检查优惠券是否已使用
        if (!"USED".equals(coupon.getStatus())) {
            log.warn("优惠券状态不是已使用，无法冲正：couponId={}, status={}", request.getCouponId(), coupon.getStatus());
            throw new CouponException("优惠券状态不是已使用，无法冲正", "INVALID_COUPON_STATUS");
        }

        // 4. 检查是否有核销记录
        CouponUsageRecordDO verifyRecord = usageRecordMapper.selectVerifyRecord(request.getCouponId(), request.getOrderId());
        if (verifyRecord == null) {
            log.warn("未找到该订单的核销记录：couponId={}, orderId={}", request.getCouponId(), request.getOrderId());
            throw new CouponException("未找到核销记录", "VERIFY_RECORD_NOT_FOUND");
        }

        // 5. 检查是否已冲正
        CouponUsageRecordDO rollbackRecord = usageRecordMapper.selectRollbackRecord(request.getCouponId(), request.getOrderId());
        if (rollbackRecord != null) {
            log.warn("优惠券已冲正：couponId={}, orderId={}", request.getCouponId(), request.getOrderId());
            // 返回成功，实现幂等
            CouponVerifyResponse response = new CouponVerifyResponse();
            response.setSuccess(true);
            response.setMsg("优惠券已冲正");
            response.setVerifyId(rollbackRecord.getRecordId());
            response.setCouponId(request.getCouponId());
            response.setOrderId(request.getOrderId());
            return response;
        }

        // 6. 检查优惠券是否过期
        String newStatus = coupon.getEndTime().after(new Date()) ? "UNUSED" : "EXPIRED";
        
        // 7. 更新优惠券状态
        int updateCount = couponMapper.updateStatus(request.getCouponId(), newStatus, new Date());
        if (updateCount == 0) {
            log.warn("优惠券状态更新失败：couponId={}", request.getCouponId());
            throw new CouponException("优惠券状态更新失败", "UPDATE_FAILED");
        }

        // 8. 记录冲正记录
        CouponUsageRecordDO usageRecord = new CouponUsageRecordDO();
        usageRecord.setRecordId(UUID.randomUUID().toString());
        usageRecord.setCouponId(request.getCouponId());
        usageRecord.setUserId(request.getUserId());
        usageRecord.setOrderId(request.getOrderId());
        usageRecord.setUsageType("ROLLBACK");
        usageRecord.setUsageTime(new Date());
        usageRecord.setRemark("订单关闭冲正：" + request.getOrderId());
        usageRecord.setCreateTime(new Date());
        usageRecordMapper.insert(usageRecord);

        log.info("优惠券冲正成功：couponId={}, orderId={}, userId={}, newStatus={}", 
                 request.getCouponId(), request.getOrderId(), request.getUserId(), newStatus);

        // 9. 返回成功响应
        CouponVerifyResponse response = new CouponVerifyResponse();
        response.setSuccess(true);
        response.setMsg("优惠券冲正成功");
        response.setVerifyId(usageRecord.getRecordId());
        response.setCouponId(request.getCouponId());
        response.setOrderId(request.getOrderId());
        return response;
    }

    /**
     * 验证优惠券状态
     */
    private void validateCouponStatus(CouponDO coupon, Long userId) {
        // 检查用户是否匹配
        if (!coupon.getUserId().equals(userId)) {
            throw new CouponException("优惠券不属于当前用户", "COUPON_NOT_BELONG_TO_USER");
        }

        // 检查状态
        if (!"UNUSED".equals(coupon.getStatus())) {
            throw new CouponException("优惠券状态不正确：" + coupon.getStatus(), "INVALID_COUPON_STATUS");
        }

        // 检查是否过期
        if (coupon.getEndTime().before(new Date())) {
            throw new CouponException("优惠券已过期", "COUPON_EXPIRED");
        }

        // 检查是否未开始
        if (coupon.getStartTime().after(new Date())) {
            throw new CouponException("优惠券尚未生效", "COUPON_NOT_STARTED");
        }
    }

    /**
     * 验证请求防重
     */
    private boolean validateRequest(String requestId, String couponId, String orderId, String operationType) {
        try {
            // 检查是否已存在该请求
            int count = requestRecordMapper.countByRequestIdAndCouponIdAndOrderId(requestId, couponId, orderId);
            if (count > 0) {
                return false;
            }

            // 插入请求记录
            CouponRequestRecordDO record = new CouponRequestRecordDO();
            record.setRequestId(requestId);
            record.setCouponId(couponId);
            record.setOrderId(orderId);
            record.setOperationType(operationType);
            record.setCreateTime(new Date());
            requestRecordMapper.insert(record);
            return true;
        } catch (Exception e) {
            // 数据库唯一约束冲突时，认为是重复请求
            return false;
        }
    }
}