package com.coding.cz.recon.entity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */
import com.coding.cz.recon.util.CompareFieldListConverter;
import com.coding.cz.recon.util.JoinFieldListConverter;
import com.coding.cz.recon.util.JsonToListConverter;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "rdc_reconciliation_rule", indexes = {
        @Index(name = "idx_task_id", columnList = "task_id")
})
public class ReconciliationRuleEntity implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "join_fields", nullable = false, columnDefinition = "json")
    @Convert(converter = JoinFieldListConverter.class) //
    private List<JoinField> joinFields; // 连接主键列表

    @Column(name = "compare_fields", nullable = false, columnDefinition = "json")
    @Convert(converter = CompareFieldListConverter.class) // 自定义转换器
    private List<CompareField> compareFields; // 比对字段列表

    private String sourceFilterConditions; // 源表过滤条件（如"status = 1 AND trans_time >= '2025-10-29'"）
    private String targetFilterConditions; // 目标表过滤条件（如"txn_status = 'SUCCESS' AND channel_time >= '2025-10-29'"）


    @Column(name = "allow_error", precision = 18, scale = 4)
    private Double allowError;

    @Column(name = "ignore_fields", length = 512)
    private String ignoreFields;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 比对字段子模型
    @Data
    public static class CompareField implements java.io.Serializable{
        private static final long serialVersionUID = 1L;
        private String sourceField;
        private String targetField;
        private String type; // 比对方式：EQUAL等
        private String fieldType; // 字段类型：DECIMAL/STRING等
    }
    @Data
    public static class JoinField implements java.io.Serializable{
        private static final long serialVersionUID = 1L;
        private String sourceField;
        private String targetField;
    }

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