package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import com.coding.cz.recon.dto.DataRecord;
import com.coding.cz.recon.dto.ReconciliationDiff;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.CoGroupFunction;
import org.apache.flink.util.Collector;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.*;


/**
 * 优化后的批量对账比对逻辑：
 * 1. 处理一对一、一对多、多对一、单边记录等所有场景
 * 2. 精确匹配连接键，避免重复比对
 * 3. 完善字段比对逻辑（空值处理、类型兼容）
 */
@Slf4j
public class BatchCoGroupFunction implements CoGroupFunction<DataRecord, DataRecord, ReconciliationDiff> {

    private final ReconciliationRuleEntity rule; // 对账规则
    private final Long taskId; // 任务ID
    private final List<ReconciliationRuleEntity.JoinField> joinFields;
    public BatchCoGroupFunction(ReconciliationRuleEntity rule, Long taskId,List<ReconciliationRuleEntity.JoinField> joinFields ) {
        this.rule = rule;
        this.taskId = taskId;
        this.joinFields = joinFields;
    }

    @Override
    public void coGroup(
            Iterable<DataRecord> sourceRecords, // 同一连接键的源表记录
            Iterable<DataRecord> targetRecords, // 同一连接键的目标表记录
            Collector<ReconciliationDiff> out // 输出差异结果
    ) {
        // 1. 转换为可操作的列表（Iterable转List）
        List<DataRecord> sourceList = new ArrayList<>();
        sourceRecords.forEach(sourceList::add);
        List<DataRecord> targetList = new ArrayList<>();
        targetRecords.forEach(targetList::add);
        log.info("coGroup触发：源表记录数=" + sourceList.size() + ", 目标表记录数=" + targetList.size());
        // 2. 连接键值（从任意一条记录取，因为按连接键分组，所有记录的连接键相同）
        String joinKeyValue = getJoinKeyValue(sourceList, targetList);
        if (joinKeyValue == null) {
            return; // 无记录，跳过
        }

        // 3. 场景1：源表有记录，目标表无 → 源表单边差异
        if (!sourceList.isEmpty() && targetList.isEmpty()) {
            for (DataRecord source : sourceList) {
                out.collect(ReconciliationDiff.sourceOnly(joinKeyValue, taskId));
            }
            return;
        }

        // 4. 场景2：目标表有记录，源表无 → 目标表单边差异
        if (sourceList.isEmpty() && !targetList.isEmpty()) {
            for (DataRecord target : targetList) {
                out.collect(ReconciliationDiff.targetOnly(joinKeyValue, taskId));
            }
            return;
        }

        log.info("两边都有记录 → 比对字段：{}", taskId);
        // 5. 场景3：两边都有记录 → 比对字段（处理一对多/多对一）
        // 用Set记录已匹配的目标表索引，避免重复匹配
        Set<Integer> matchedTargetIndices = new HashSet<>();

        // 遍历源表记录，尝试匹配目标表记录
        for (DataRecord source : sourceList) {
            boolean isMatched = false;

            // 遍历目标表记录，寻找可匹配的记录
            for (int i = 0; i < targetList.size(); i++) {
                if (matchedTargetIndices.contains(i)) {
                    continue; // 已匹配的目标记录跳过
                }

                DataRecord target = targetList.get(i);
                // 字段完全匹配 → 标记为已匹配，不再参与其他源记录的比对
                if (isFieldsMatched(source, target,out)) {
                    matchedTargetIndices.add(i);
                    isMatched = true;
                    break;
                }
            }

            // 源表记录未找到匹配的目标记录 → 视为源表单边差异
            if (!isMatched) {
                out.collect(ReconciliationDiff.sourceOnly(joinKeyValue, taskId));
            }
        }

        // 遍历目标表记录，未匹配的视为目标表单边差异
        for (int i = 0; i < targetList.size(); i++) {
            if (!matchedTargetIndices.contains(i)) {
                out.collect(ReconciliationDiff.targetOnly(joinKeyValue, taskId));
            }
        }
    }

    /**
     * 获取连接键值（从源表或目标表记录中提取）
     */
    private String getJoinKeyValue(List<DataRecord> sourceList, List<DataRecord> targetList) {
        if (!sourceList.isEmpty()) {
            return sourceList.get(0).getJoinKey(joinFields, true); // 源表
        } else if (!targetList.isEmpty()) {
            return targetList.get(0).getJoinKey(joinFields, false); // 目标表
        } else {
            return null;
        }
    }

    /**
     * 比对两个记录的字段是否匹配（核心比对逻辑）
     */
    private boolean isFieldsMatched(DataRecord source, DataRecord target,Collector<ReconciliationDiff> collector) {
        for (ReconciliationRuleEntity.CompareField compareField : rule.getCompareFields()) {
            String sourceField = compareField.getSourceField();
            String targetField = compareField.getTargetField();

            // 获取字段值（处理空值）
            String sourceVal = source.getField(sourceField) == null ? "" : source.getField(sourceField);
            String targetVal = target.getField(targetField) == null ? "" : target.getField(targetField);

            // 字段值不匹配 → 记录差异并返回false
            if (!isValueMatched(sourceVal, targetVal, compareField)) {
                // 输出字段不匹配的差异
                collector.collect(ReconciliationDiff.fieldMismatch(
                        source.getJoinKey(rule.getJoinFields(), true),
                        sourceField + ":" + targetField,
                        sourceVal,
                        targetVal,
                        taskId
                ));
                return false;
            }
        }
        // 所有字段匹配
        return true;
    }

    /**
     * 比对单个字段的值（支持不同类型和误差范围）
     */
    private boolean isValueMatched(String sourceVal, String targetVal, ReconciliationRuleEntity.CompareField field) {
        // 空值统一处理（都为空视为匹配）
        if (sourceVal.isEmpty() && targetVal.isEmpty()) {
            return true;
        }

        // 数字类型（支持误差范围）
        if ("DECIMAL".equals(field.getFieldType()) || "NUMBER".equals(field.getFieldType())) {
            try {
                double sourceNum = Double.parseDouble(sourceVal);
                double targetNum = Double.parseDouble(targetVal);
                // 允许的误差范围（默认0.0000）
                double allowError = rule.getAllowError() != null ? rule.getAllowError().doubleValue() : 0.0000;
                return Math.abs(sourceNum - targetNum) <= allowError;
            } catch (NumberFormatException e) {
                // 转换失败视为不匹配（如字符串无法转数字）
                return false;
            }
        }

        // 字符串类型（精确匹配）
        if ("STRING".equals(field.getFieldType()) || "VARCHAR".equals(field.getFieldType())) {
            return sourceVal.equals(targetVal);
        }

        // 日期时间类型（格式统一后匹配，如"yyyy-MM-dd HH:mm:ss"）
        if ("DATETIME".equals(field.getFieldType())) {
            // 简化处理：假设格式一致，直接比对字符串
            return sourceVal.equals(targetVal);
        }

        // 其他类型默认精确匹配
        return sourceVal.equals(targetVal);
    }
}
