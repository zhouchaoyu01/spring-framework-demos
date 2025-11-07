package com.coding.cz.recon.dto;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import lombok.*;

/**
 * 对账差异记录载体
 */

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Flink对账过程中产生的差异信息（非数据库实体，仅内存中传递）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconciliationDiff implements java.io.Serializable{
    public static final long serialVersionUID = 1L;


    // 差异类型：SOURCE_ONLY（源表单边）、TARGET_ONLY（目标表单边）、FIELD_MISMATCH（字段不匹配）
    private String type;
    // 连接主键组合值（如"123"或"1001_2025001"）
    private String joinKeyValue;
    // 不匹配的字段名（仅FIELD_MISMATCH时有值，如"amount"）
    private String mismatchField;
    // 源表字段值（仅FIELD_MISMATCH时有值）
    private String sourceValue;
    // 目标表字段值（仅FIELD_MISMATCH时有值）
    private String targetValue;
    // 关联的任务ID
    private Long taskId;

    // 工厂方法：创建源表单边差异
    public static ReconciliationDiff sourceOnly(String joinKeyValue, Long taskId) {
        return new ReconciliationDiff("SOURCE_ONLY", joinKeyValue, null, null, null, taskId);
    }

    // 工厂方法：创建目标表单边差异
    public static ReconciliationDiff targetOnly(String joinKeyValue, Long taskId) {
        return new ReconciliationDiff("TARGET_ONLY", joinKeyValue, null, null, null, taskId);
    }

    // 工厂方法：创建字段不匹配差异
    public static ReconciliationDiff fieldMismatch(
            String joinKeyValue,
            String mismatchField,
            String sourceValue,
            String targetValue,
            Long taskId
    ) {
        return new ReconciliationDiff("FIELD_MISMATCH", joinKeyValue, mismatchField, sourceValue, targetValue, taskId);
    }
}
