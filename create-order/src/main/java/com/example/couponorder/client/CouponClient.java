package com.example.couponorder.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 优惠券服务客户端
 */
@FeignClient(name = "coupon-service", url = "${coupon.service.url:http://localhost:8081}")
public interface CouponClient {

    /**
     * 核销优惠券
     */
    @PostMapping("/api/coupon/verify")
    CouponVerifyResp verifyCoupon(@RequestBody CouponVerifyReq req);

    /**
     * 冲正优惠券
     */
    @PostMapping("/api/coupon/rollback")
    CouponRollbackResp rollbackCoupon(@RequestBody CouponRollbackReq req);
}