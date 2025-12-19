package com.cz.v2.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-12-17
 */


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠券实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    private String couponId;     // 优惠券ID
    private String userId;       // 用户ID
    private BigDecimal amount;   // 优惠金额
    private String status;       // 状态 (AVAILABLE/USED/EXPIRED)
    private Long expireTime;     // 过期时间
}
