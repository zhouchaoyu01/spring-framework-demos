package com.coding.cz.recon.constant;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {
    ACTIVE(1, "运行中"),
    INACTIVE(2, "停用"),
    ERROR(3, "异常");

    private final Integer code;
    private final String desc;
}