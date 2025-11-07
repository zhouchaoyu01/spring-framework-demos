package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rdc_execution_record", indexes = {
        @Index(name = "idx_task_id", columnList = "task_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_start_time", columnList = "start_time")
})
public class ExecutionRecordEntity implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_no", nullable = false, unique = true, length = 50)
    private String executionNo;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Integer type; // 1-手动，2-定时，3-事件

    @Column(nullable = false)
    private Integer status; // 1-处理中，2-成功，3-失败

    @Column(name = "total_count")
    private Long totalCount;

    @Column(name = "match_count")
    private Long matchCount;

    @Column(name = "difference_count")
    private Long differenceCount;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "cost_ms")
    private Long costMs;

    @Column(name = "error_msg", columnDefinition = "text")
    private String errorMsg;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.startTime = LocalDateTime.now(); // 默认开始时间为创建时间
    }
}