package com.example.coupon.entity;

import lombok.Data;

import java.util.Date;

/**
 * 优惠券使用记录实体类
 */
@Data
public class CouponUsageRecordDO {
    private Long id;
    private String recordId;
    private String couponId;
    private Long userId;
    private String orderId;
    private String usageType;
    private Date usageTime;
    private String remark;
    private Date createTime;
}