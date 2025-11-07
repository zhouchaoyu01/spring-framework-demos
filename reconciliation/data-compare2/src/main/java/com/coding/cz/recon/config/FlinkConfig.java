package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "flink")
public class FlinkConfig {
    private Integer parallelism = 8; // 并行度
    private Long checkpointInterval = 60000L; // Checkpoint间隔（毫秒）
    private String stateBackend = "rocksdb"; // 状态后端
    private String checkpointDir = "hdfs:///flink/checkpoints"; // Checkpoint存储路径
}
