package com.coding.cz.recon.dto;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 源表/目标表的单条记录载体
 */

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flink处理中封装的单条原始数据（非数据库实体，仅内存中使用）
 */
@Data
public class DataRecord implements java.io.Serializable{

    public static final long serialVersionUID = 1L;

    // 存储字段名→字段值的映射（如{"trans_id":"123","amount":"100.00"}）
    private final Map<String, String> fields = new HashMap<>();

    // 设置字段值
    public void setField(String fieldName, String value) {
        fields.put(fieldName, value);
    }

    // 获取字段值
    public String getField(String fieldName) {
        return fields.getOrDefault(fieldName, "");
    }


    /**
     * 根据连接条件生成连接键（支持多字段）
     * @param joinFields 连接条件列表（来自rule.joinFields）
     * @param isSource 是否为源表（true：用sourceField；false：用targetField）
     */
    public String getJoinKey(List<ReconciliationRuleEntity.JoinField> joinFields, boolean isSource) {
        StringBuilder key = new StringBuilder();
        for (ReconciliationRuleEntity.JoinField joinField : joinFields) {
            // 源表取sourceField，目标表取targetField
            String fieldName = isSource ? joinField.getSourceField() : joinField.getTargetField();
            if (key.length() > 0) {
                key.append("_"); // 固定分隔符，确保多字段拼接唯一
            }
            key.append(getField(fieldName)); // 拼接字段值
        }
        return key.toString();
    }

    public String getJoinKeyFieldName(List<ReconciliationRuleEntity.JoinField> joinFields, boolean isSource) {
        StringBuilder key = new StringBuilder();
        for (ReconciliationRuleEntity.JoinField joinField : joinFields) {
            // 源表取sourceField，目标表取targetField
            String fieldName = isSource ? joinField.getSourceField() : joinField.getTargetField();
            if (key.length() > 0) {
                key.append("_"); // 固定分隔符，确保多字段拼接唯一
            }
            key.append(fieldName); // 拼接字段值
        }
        return key.toString();
    }
}