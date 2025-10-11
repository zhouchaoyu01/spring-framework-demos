package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rdc_reconciliation_diff")
public class RdcReconciliationDiff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对账任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 执行批次号 */
    @Column(name = "batch_no", length = 100)
    private String batchNo;

    /** 对账类型（交易对账、资金对账等） */
    @Column(name = "reconciliation_type", length = 50)
    private String reconciliationType;

    /** 对账键（订单号、交易号等） */
    @Column(name = "reconciliation_key", length = 100)
    private String reconciliationKey;

    /** 来源1名称 */
    @Column(name = "source1_name", length = 100)
    private String source1Name;

    /** 来源2名称 */
    @Column(name = "source2_name", length = 100)
    private String source2Name;

    /** 来源1金额 */
    @Column(name = "source1_amount", precision = 18, scale = 2)
    private BigDecimal source1Amount;

    /** 来源2金额 */
    @Column(name = "source2_amount", precision = 18, scale = 2)
    private BigDecimal source2Amount;

    /** 来源1状态 */
    @Column(name = "source1_status", length = 50)
    private String source1Status;

    /** 来源2状态 */
    @Column(name = "source2_status", length = 50)
    private String source2Status;

    /** 差错类型（长款、短款、金额不符等） */
    @Column(name = "diff_type", length = 50)
    private String diffType;

    /** 差错方向（收入 / 支出） */
    @Column(name = "direction", length = 10)
    private String direction;

    /** 币种 */
    @Column(name = "currency", length = 10)
    private String currency;

    /** 差额金额 */
    @Column(name = "diff_amount", precision = 18, scale = 2)
    private BigDecimal diffAmount;

    /** 是否已处理 0未处理 1已处理 */
    @Column(name = "resolved_flag")
    private Boolean resolvedFlag;

    /** 备注 */
    @Column(name = "remark", length = 255)
    private String remark;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
