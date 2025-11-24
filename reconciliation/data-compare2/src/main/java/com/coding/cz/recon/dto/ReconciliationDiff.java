package com.coding.cz.recon.dto;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

/**
 * 对账差异记录载体
 */

import com.coding.cz.recon.FlinkReconciliationJob;
import lombok.AllArgsConstructor;
import lombok.Data;


        import lombok.Builder;
        import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 对账差异结果实体
 * 统一所有差异类型的输出格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class ReconciliationDiff implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String diffType;
    private String joinKeyValue;
    private String mismatchField;
    private String sourceValue;
    private String targetValue;
    private Long taskId;
    private String description;

    public static ReconciliationDiff match(String joinKeyValue, Long taskId) {
        return ReconciliationDiff.builder().diffType("MATCH").joinKeyValue(joinKeyValue).taskId(taskId).description("记录匹配").build();
    }

    public static ReconciliationDiff fieldMismatch(String joinKeyValue, String mismatchField, String sourceValue, String targetValue, Long taskId) {
        return ReconciliationDiff.builder().diffType("FIELD_MISMATCH").joinKeyValue(joinKeyValue).mismatchField(mismatchField)
                .sourceValue(sourceValue).targetValue(targetValue).taskId(taskId).description("字段不匹配").build();
    }

    public static ReconciliationDiff sourceOnly(String joinKeyValue, Long taskId) {
        return ReconciliationDiff.builder().diffType("SOURCE_ONLY").joinKeyValue(joinKeyValue).taskId(taskId).description("源表有记录，目标表无记录").build();
    }

    public static ReconciliationDiff targetOnly(String joinKeyValue, Long taskId) {
        return ReconciliationDiff.builder().diffType("TARGET_ONLY").joinKeyValue(joinKeyValue).taskId(taskId).description("目标表有记录，源表无记录").build();
    }
}

