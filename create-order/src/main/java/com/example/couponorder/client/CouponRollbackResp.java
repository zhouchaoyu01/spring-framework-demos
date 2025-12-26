package com.example.couponorder.client;

import lombok.Data;

/**
 * 优惠券冲正响应
 */
@Data
public class CouponRollbackResp {
    private boolean success;
    private String msg;
    private String rollbackId;
}