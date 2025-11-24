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
@Table(name = "rdc_reconciliation_task", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_type", columnList = "type"),
        @Index(name = "idx_source_ds", columnList = "source_data_source_id"),
        @Index(name = "idx_target_ds", columnList = "target_data_source_id")
})
public class ReconciliationTaskEntity implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer type; // 1-实时，2-批量

    @Column(name = "source_data_source_id", nullable = false)
    private Long sourceDataSourceId;

    @Column(name = "target_data_source_id", nullable = false)
    private Long targetDataSourceId;

    @Column(name = "source_table", nullable = false, length = 100)
    private String sourceTable;

    @Column(name = "target_table", nullable = false, length = 100)
    private String targetTable;

    @Column(nullable = false)
    private Integer status; // 1-运行中，2-停用，3-异常

    private String description;

    @Column(name = "create_user_id", nullable = false)
    private Long createUserId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Transient
    private DataSourceEntity sourceDataSource;

    @Transient
    private DataSourceEntity targetDataSource;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}