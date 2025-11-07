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
@Table(name = "rdc_difference_record", indexes = {
        @Index(name = "idx_task_id", columnList = "task_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_discovery_time", columnList = "discovery_time")
})
public class DifferenceRecordEntity implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "difference_no", nullable = false, unique = true, length = 50)
    private String differenceNo;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 50)
    private String type; // SOURCE_ONLY/TARGET_ONLY/AMOUNT_MISMATCH等

    @Column(name = "source_data_id", length = 100)
    private String sourceDataId;

    @Column(name = "target_data_id", length = 100)
    private String targetDataId;

    @Column(name = "mismatch_field", length = 100)
    private String mismatchField;

    @Column(name = "source_value", columnDefinition = "text")
    private String sourceValue;

    @Column(name = "target_value", columnDefinition = "text")
    private String targetValue;

    @Column(nullable = false)
    private Integer status; // 1-待处理，2-已处理，3-已忽略

    @Column(name = "discovery_time", nullable = false)
    private LocalDateTime discoveryTime;

    @Column(name = "handle_user_id")
    private Long handleUserId;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "handle_remark", columnDefinition = "text")
    private String handleRemark;

    @PrePersist
    public void prePersist() {
        this.discoveryTime = LocalDateTime.now();
    }
}