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
public enum ExecutionStatusEnum {
    //1-处理中；2-成功；3-失败
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAIL(3, "失败");

    private final Integer code;
    private final String desc;
}