package com.example.couponorder.client;

import lombok.Data;

/**
 * 优惠券核销响应
 */
@Data
public class CouponVerifyResp {
    private boolean success;
    private String msg;
    private String verifyId;
}