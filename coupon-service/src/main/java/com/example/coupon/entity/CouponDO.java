package com.example.coupon.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券实体类
 */
@Data
public class CouponDO {
    private Long id;
    private String couponId;
    private Long userId;
    private String couponCode;
    private String couponType;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private String status;
    private Date startTime;
    private Date endTime;
    private Date createTime;
    private Date updateTime;
}