package com.example.couponorder.entity;

import lombok.Data;

import java.util.Date;

/**
 * 订单实体类
 */
@Data
public class OrderDO {
    private String orderId;
    private Long userId;
    private String couponId;
    private String status;
    private Date createTime;
    private Date updateTime;
}