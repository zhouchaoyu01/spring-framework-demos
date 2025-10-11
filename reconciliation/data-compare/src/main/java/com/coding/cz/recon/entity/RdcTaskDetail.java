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
@Table(name = "rdc_task_detail")
public class RdcTaskDetail {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对账任务ID */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 数据源1（如kafka、mysql、doris） */
    @Column(name = "source1", length = 50)
    private String source1;

    /** 数据源2（如mysql、doris） */
    @Column(name = "source2", length = 50)
    private String source2;

    /** 数据源1配置（连接信息、topic、表名等） */
    @Column(name = "source1_config", columnDefinition = "TEXT")
    private String source1Config;

    /** 数据源2配置（连接信息、topic、表名等） */
    @Column(name = "source2_config", columnDefinition = "TEXT")
    private String source2Config;

    /** 原始对账SQL模板，可包含动态参数，如  $ {bizDate} */
    @Column(name = "sql_template", length = 255)
    private String sqlTemplate;
    /** 任务描述 */
    @Column(name = "description", length = 255)
    private String description;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
