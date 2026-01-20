package com.coding.rocketmq;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-08
 */


import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderMessage implements Serializable {
    private String orderId;
    private String userId;
    private String productId;
    private Integer quantity;
    private BigDecimal amount;
    private LocalDateTime createTime;
    private String status;
}
