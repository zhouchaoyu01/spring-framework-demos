package com.example.couponorder.client;

import lombok.Data;

/**
 * 优惠券核销请求
 */
@Data
public class CouponVerifyReq {
    private String couponId;
    private String orderId;
    private Long userId;
}