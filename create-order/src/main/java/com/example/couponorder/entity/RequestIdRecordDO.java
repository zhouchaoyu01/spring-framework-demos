package com.example.couponorder.entity;

import lombok.Data;

import java.util.Date;

/**
 * 请求防重记录实体类
 */
@Data
public class RequestIdRecordDO {
    private Long id;
    private String requestId;
    private Long userId;
    private String bizType;
    private Date createTime;
}