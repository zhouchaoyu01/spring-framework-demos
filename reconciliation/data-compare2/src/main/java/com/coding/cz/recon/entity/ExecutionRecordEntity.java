package com.coding.cz.recon.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账执行记录实体
 * 记录每次对账任务的执行状态和统计信息
 */
@Data
@Entity
@Table(name = "rdc_reconciliation_execution_record")
public class ExecutionRecordEntity implements java.io.Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对账任务ID
     */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 对账日期（T-1日）
     */
    @Column(name = "recon_date", nullable = false)
    private LocalDate reconDate;

    /**
     * 执行状态：0-初始化，1-执行中，2-成功，3-失败
     */
    @Column(name = "status", nullable = false)
    private Integer status;

    /**
     * 源表总记录数
     */
    @Column(name = "source_total_count")
    private Long sourceTotalCount;

    /**
     * 目标表总记录数
     */
    @Column(name = "target_total_count")
    private Long targetTotalCount;

    /**
     * 匹配记录数
     */
    @Column(name = "matched_count")
    private Long matchedCount;

    /**
     * 字段不匹配记录数
     */
    @Column(name = "field_mismatch_count")
    private Long fieldMismatchCount;

    /**
     * 源表单边记录数
     */
    @Column(name = "source_only_count")
    private Long sourceOnlyCount;

    /**
     * 目标表单边记录数
     */
    @Column(name = "target_only_count")
    private Long targetOnlyCount;

    /**
     * 开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "execution_time")
    private Long executionTime;

    /**
     * 错误信息（失败时记录）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = 0; // 默认初始化状态
        }
        if (matchedCount == null) matchedCount = 0L;
        if (fieldMismatchCount == null) fieldMismatchCount = 0L;
        if (sourceOnlyCount == null) sourceOnlyCount = 0L;
        if (targetOnlyCount == null) targetOnlyCount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
