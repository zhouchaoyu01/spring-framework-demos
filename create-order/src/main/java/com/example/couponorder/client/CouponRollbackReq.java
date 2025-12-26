package com.example.couponorder.client;

import lombok.Data;

/**
 * 优惠券冲正请求
 */
@Data
public class CouponRollbackReq {
    private String couponId;
    private String orderId;
    private String reason;
}