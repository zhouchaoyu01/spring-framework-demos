package com.example.coupon.dto;

import lombok.Data;

/**
 * 优惠券核销响应DTO
 */
@Data
public class CouponVerifyResponse {
    private boolean success;
    private String msg;
    private String verifyId;
    private String couponId;
    private String orderId;
}