package com.example.couponorder.entity;

import lombok.Data;

import java.util.Date;

/**
 * 订单-优惠券核销记录实体类
 */
@Data
public class OrderCouponVerifyDO {
    private Long id;
    private String orderId;
    private String couponId;
    private String verifyStatus;
    private Date verifyTime;
    private Date rollbackTime;
    private String extMsg;
    private String uniqueKey;
    private Date createTime;
    private Date updateTime;
}