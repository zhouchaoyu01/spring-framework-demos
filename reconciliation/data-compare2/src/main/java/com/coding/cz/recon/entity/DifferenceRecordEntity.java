package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-11-12
 */


import com.coding.cz.recon.dto.ReconciliationDiff;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账差异记录表实体类
 * 对应数据库表：rdc_reconciliation_difference_record
 */
@Data
@Entity
@Table(name = "rdc_reconciliation_difference_record")
public class DifferenceRecordEntity implements java.io.Serializable{

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联执行记录ID
     */
    @Column(name = "execution_record_id", nullable = false)
    private Long executionRecordId;

    /**
     * 差异类型：
     * - MATCH: 匹配
     * - FIELD_MISMATCH: 字段不匹配
     * - SOURCE_ONLY: 源表有记录，目标表无记录
     * - TARGET_ONLY: 目标表有记录，源表无记录
     */
    @Column(name = "diff_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiffType diffType;

    /**
     * 连接键值（如order_id的值）
     */
    @Column(name = "join_key_value", nullable = false, length = 200)
    private String joinKeyValue;

    /**
     * 不匹配的字段名（多个用逗号分隔）
     */
    @Column(name = "mismatch_field", length = 200)
    private String mismatchField;

    /**
     * 源表字段值
     */
    @Column(name = "source_value", length = 500)
    private String sourceValue;

    /**
     * 目标表字段值
     */
    @Column(name = "target_value", length = 500)
    private String targetValue;

    /**
     * 对账任务ID
     */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 差异描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 差异类型枚举
     */
    public enum DiffType {
        MATCH,              // 匹配
        FIELD_MISMATCH,     // 字段不匹配
        SOURCE_ONLY,        // 源表有记录，目标表无记录
        TARGET_ONLY         // 目标表有记录，源表无记录
    }

    /**
     * 自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    /**
     * 从ReconciliationDiff转换为实体类
     */
    public static DifferenceRecordEntity fromDiff(ReconciliationDiff diff, Long executionRecordId) {
        DifferenceRecordEntity entity = new DifferenceRecordEntity();
        entity.setExecutionRecordId(executionRecordId);
        entity.setDiffType(DiffType.valueOf(diff.getDiffType()));
        entity.setJoinKeyValue(diff.getJoinKeyValue());
        entity.setMismatchField(diff.getMismatchField());
        entity.setSourceValue(diff.getSourceValue());
        entity.setTargetValue(diff.getTargetValue());
        entity.setTaskId(diff.getTaskId());
        entity.setDescription(diff.getDescription());
        return entity;
    }
}
