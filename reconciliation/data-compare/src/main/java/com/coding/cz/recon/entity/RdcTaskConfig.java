package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rdc_task_config")
public class RdcTaskConfig {

    /** 对账任务ID（主键） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 对账任务编码 */
    @Column(name = "task_code", nullable = false, length = 50)
    private String taskCode;

    /** 对账任务名称 */
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    /** 对账类型（交易对账、资金对账等） */
    @Column(name = "task_type", length = 50)
    private String taskType;

    /** 启用对账日期 */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /** 定时任务执行时间（如cron表达式） */
    @Column(name = "schedule_time", length = 100)
    private String scheduleTime;

    /** 对账模式（实时 / 离线） */
    @Column(name = "mode", length = 20)
    private String mode;

    /** 任务状态（启用、禁用） */
    @Column(name = "status", length = 10)
    private String status;

    /** 商户号（可选） */
    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
