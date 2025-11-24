package com.coding.cz.recon.service.flink.job;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-11-12
 */


import com.coding.cz.recon.entity.DataSourceEntity;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;
import com.coding.cz.recon.repository.DataSourceRepository;
import com.coding.cz.recon.repository.ReconciliationRuleRepository;
import com.coding.cz.recon.repository.ReconciliationTaskRepository;

import com.coding.cz.recon.service.flink.processor.JoinBasedReconciliationProcessor;
import com.coding.cz.recon.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 基于双流Join的对账Job启动类
 * 替代原有的BatchReconciliationJob，使用Join算子进行对账
 */
@Slf4j
@Component
public class JoinBasedReconciliationJob implements CommandLineRunner {

    // 对账任务ID（可通过配置文件或命令行参数传入）
    private static final Long TASK_ID = 1L;
    // 对账日期（T-1日，这里以当前日期的前一天为例）
    private final LocalDate RECON_DATE = LocalDate.of(2025, 5, 6);

    @Override
    public void run(String... args) throws Exception {
        log.info("基于Join的对账Job开始启动，任务ID: {}, 对账日期: {}", TASK_ID, RECON_DATE);

        try {
            // 1. 初始化Spring上下文（获取数据库访问Bean）
            ReconciliationTaskRepository taskRepository = SpringContextHolder.getBean(ReconciliationTaskRepository.class);
            ReconciliationRuleRepository ruleRepository = SpringContextHolder.getBean(ReconciliationRuleRepository.class);
            DataSourceRepository dataSourceRepository = SpringContextHolder.getBean(DataSourceRepository.class);

            // 2. 加载对账任务配置
            ReconciliationTaskEntity task = taskRepository.findById(TASK_ID)
                    .orElseThrow(() -> new RuntimeException("任务ID=" + TASK_ID + "不存在"));

            // 3. 加载对账规则配置
            ReconciliationRuleEntity rule = ruleRepository.findByTaskId(TASK_ID)
                    .orElseThrow(() -> new RuntimeException("任务ID=" + TASK_ID + "未配置规则"));

            // 4. 加载源数据源和目标数据源配置
            DataSourceEntity sourceDataSource = dataSourceRepository.findById(task.getSourceDataSourceId())
                    .orElseThrow(() -> new RuntimeException("源数据源ID=" + task.getSourceDataSourceId() + "不存在"));
            DataSourceEntity targetDataSource = dataSourceRepository.findById(task.getTargetDataSourceId())
                    .orElseThrow(() -> new RuntimeException("目标数据源ID=" + task.getTargetDataSourceId() + "不存在"));

            // 5. 初始化Flink执行环境
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            // 批量任务可适当调整并行度
            env.setParallelism(1);
            // 启用Checkpoint（可选，用于故障恢复）
            // env.enableCheckpointing(300000); // 5分钟一次Checkpoint

            // 6. 生成执行记录ID（简化处理，实际应从数据库生成）
            Long executionRecordId = System.currentTimeMillis();

            // 7. 创建基于Join的对账处理器并执行
            JoinBasedReconciliationProcessor processor = new JoinBasedReconciliationProcessor(
                    env,
                    sourceDataSource,
                    targetDataSource,
                    task,
                    rule,
                    RECON_DATE,
                    executionRecordId
            );
            processor.process();

            // 8. 启动Flink作业
            log.info("基于Join的对账作业启动，任务ID: {}, 对账日期: {}", TASK_ID, RECON_DATE);
            env.execute("Join-Based-Reconciliation-Job-" + TASK_ID + "-" + RECON_DATE);

        } catch (Exception e) {
            log.error("基于Join的对账Job启动失败", e);
            throw e;
        }
    }
}

