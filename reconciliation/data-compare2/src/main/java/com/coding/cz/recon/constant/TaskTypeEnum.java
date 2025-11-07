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
public enum TaskTypeEnum {
    REAL_TIME(1, "实时对账"),
    BATCH(2, "批量对账");

    private final Integer code;
    private final String desc;
}