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
@Table(name = "rdc_data_source", indexes = {
        @Index(name = "idx_status", columnList = "status")
})
public class DataSourceEntity implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type; // 如"MYSQL"、"KAFKA"

    @Column(name = "connection_params", nullable = false, columnDefinition = "json")
    private String connectionParams; // JSON字符串

    @Column(nullable = false)
    private Integer status; // 1-可用，0-不可用

    private String remark;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

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
