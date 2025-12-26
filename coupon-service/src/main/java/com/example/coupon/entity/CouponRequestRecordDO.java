package com.example.coupon.entity;

import lombok.Data;

import java.util.Date;

/**
 * 优惠券请求防重记录实体类
 */
@Data
public class CouponRequestRecordDO {
    private Long id;
    private String requestId;
    private String couponId;
    private String orderId;
    private String operationType;
    private Date createTime;
}