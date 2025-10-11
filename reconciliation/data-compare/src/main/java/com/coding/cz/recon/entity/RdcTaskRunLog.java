package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rdc_task_run_log")
public class RdcTaskRunLog {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对账任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 执行批次号 */
    @Column(name = "batch_no", length = 100)
    private String batchNo;
    /** 业务日期 */
    @Column(name = "biz_date")
    private LocalDate bizDate;

    /** 执行状态（成功、失败、部分成功） */
    @Column(name = "status", length = 20)
    private String status;

    /** 对账起始时间 */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /** 对账结束时间 */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /** 记录总数 */
    @Column(name = "record_count")
    private Long recordCount;

    /** 差错总数 */
    @Column(name = "diff_count")
    private Long diffCount;

    /** 错误信息 */
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;
}

