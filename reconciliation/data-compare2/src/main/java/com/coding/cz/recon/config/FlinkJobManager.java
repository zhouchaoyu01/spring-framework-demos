package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.job.ReconciliationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Component
@RequiredArgsConstructor
public class FlinkJobManager {

    private final FlinkConfig flinkConfig;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 提交对账作业
     */
    public void submitJob(Long taskId, LocalDate t1Date, Long executionRecordId) {
        // 1. 初始化Flink环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        configureFlinkEnv(env);

        // 2. 提交作业（异步执行，避免阻塞业务线程）
//        executor.submit(new ReconciliationJob(
//                env,
//                taskId,
//                t1Date,
//                executionRecordId
//        ));
    }

    /**
     * 配置Flink环境
     */
    private void configureFlinkEnv(StreamExecutionEnvironment env) {
        env.setParallelism(flinkConfig.getParallelism());
    }
}
