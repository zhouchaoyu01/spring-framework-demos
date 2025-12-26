package com.example.coupon.dto;

import lombok.Data;

/**
 * 优惠券核销请求DTO
 */
@Data
public class CouponVerifyRequest {
    private String couponId;
    private String orderId;
    private Long userId;
    private String requestId;
}