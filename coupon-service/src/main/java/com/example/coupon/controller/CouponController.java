package com.example.coupon.controller;

import com.example.coupon.dto.CouponVerifyRequest;
import com.example.coupon.dto.CouponVerifyResponse;
import com.example.coupon.exception.CouponException;
import com.example.coupon.service.CouponVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponVerifyService couponVerifyService;

    /**
     * 核销优惠券
     */
    @PostMapping("/verify")
    public CouponVerifyResponse verifyCoupon(@RequestBody CouponVerifyRequest request) {
        try {
            log.info("收到优惠券核销请求：couponId={}, orderId={}, userId={}", 
                     request.getCouponId(), request.getOrderId(), request.getUserId());
            
            // 生成请求ID（如果客户端没有提供）
            if (request.getRequestId() == null) {
                request.setRequestId(java.util.UUID.randomUUID().toString());
            }
            
            CouponVerifyResponse response = couponVerifyService.verifyCoupon(request);
            log.info("优惠券核销成功：couponId={}, orderId={}", request.getCouponId(), request.getOrderId());
            return response;
        } catch (CouponException e) {
            log.warn("优惠券核销失败：couponId={}, orderId={}, error={}, message={}", 
                     request.getCouponId(), request.getOrderId(), e.getErrorCode(), e.getMessage());
            CouponVerifyResponse response = new CouponVerifyResponse();
            response.setSuccess(false);
            response.setMsg(e.getMessage());
            response.setCouponId(request.getCouponId());
            response.setOrderId(request.getOrderId());
            return response;
        } catch (Exception e) {
            log.error("优惠券核销异常：couponId={}, orderId={}", request.getCouponId(), request.getOrderId(), e);
            CouponVerifyResponse response = new CouponVerifyResponse();
            response.setSuccess(false);
            response.setMsg("系统异常，请稍后重试");
            response.setCouponId(request.getCouponId());
            response.setOrderId(request.getOrderId());
            return response;
        }
    }

    /**
     * 冲正优惠券
     */
    @PostMapping("/rollback")
    public CouponVerifyResponse rollbackCoupon(@RequestBody CouponVerifyRequest request) {
        try {
            log.info("收到优惠券冲正请求：couponId={}, orderId={}, userId={}", 
                     request.getCouponId(), request.getOrderId(), request.getUserId());
            
            // 生成请求ID（如果客户端没有提供）
            if (request.getRequestId() == null) {
                request.setRequestId(java.util.UUID.randomUUID().toString());
            }
            
            CouponVerifyResponse response = couponVerifyService.rollbackCoupon(request);
            log.info("优惠券冲正成功：couponId={}, orderId={}", request.getCouponId(), request.getOrderId());
            return response;
        } catch (CouponException e) {
            log.warn("优惠券冲正失败：couponId={}, orderId={}, error={}, message={}", 
                     request.getCouponId(), request.getOrderId(), e.getErrorCode(), e.getMessage());
            CouponVerifyResponse response = new CouponVerifyResponse();
            response.setSuccess(false);
            response.setMsg(e.getMessage());
            response.setCouponId(request.getCouponId());
            response.setOrderId(request.getOrderId());
            return response;
        } catch (Exception e) {
            log.error("优惠券冲正异常：couponId={}, orderId={}", request.getCouponId(), request.getOrderId(), e);
            CouponVerifyResponse response = new CouponVerifyResponse();
            response.setSuccess(false);
            response.setMsg("系统异常，请稍后重试");
            response.setCouponId(request.getCouponId());
            response.setOrderId(request.getOrderId());
            return response;
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public String health() {
        return "coupon-service is running";
    }
}