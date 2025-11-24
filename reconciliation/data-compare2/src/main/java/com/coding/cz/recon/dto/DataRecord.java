package com.coding.cz.recon.dto;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.FlinkReconciliationJob;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import lombok.Data;

import java.util.*;

/**
 * 源表/目标表的单条记录载体
 */

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flink处理中封装的单条原始数据（非数据库实体，仅内存中使用）
 */
@Data
@Slf4j
public  class DataRecord implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> fields = new HashMap<>();

    public DataRecord() {
    }

    public DataRecord(Map<String, String> map) {
        if (map != null) this.fields.putAll(map);
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getJoinKey(List<ReconciliationRuleEntity.JoinField> joinFields, boolean isSource) {
        List<String> parts = new ArrayList<>();
        for (ReconciliationRuleEntity.JoinField jf : joinFields) {
            String f = isSource ? jf.getSourceField() : jf.getTargetField();
            parts.add(String.valueOf(fields.get(f)));
        }
        return String.join("|", parts);
    }

}