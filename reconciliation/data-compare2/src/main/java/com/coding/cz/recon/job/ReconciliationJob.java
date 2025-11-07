package com.coding.cz.recon.job;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.config.BatchReconciliationProcessor;
import com.coding.cz.recon.config.DataSourceReader;

import com.coding.cz.recon.constant.ExecutionStatusEnum;
import com.coding.cz.recon.constant.TaskTypeEnum;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;
import com.coding.cz.recon.service.ExecutionRecordService;
import com.coding.cz.recon.service.RuleService;
import com.coding.cz.recon.service.TaskService;
import com.coding.cz.recon.util.SpringContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
//@RequiredArgsConstructor
public class ReconciliationJob implements CommandLineRunner {

//    private final StreamExecutionEnvironment env;
    private final Long taskId = 1L;
    private final LocalDate t1Date = LocalDate.of(2025, 5, 9);
//    private final Long executionRecordId;

    @Override
    public void run(String ...args) {
        Long executionRecordId = new Random(10).nextLong();
        System.out.println("executionRecordId:" + executionRecordId);
        try {
            // 1. 获取Spring服务（通过上下文工具类）
            TaskService taskService = SpringContextHolder.getBean(TaskService.class);
            RuleService ruleService = SpringContextHolder.getBean(RuleService.class);
            DataSourceReader dataSourceReader = SpringContextHolder.getBean(DataSourceReader.class);

            // 2. 加载任务和规则元数据
            ReconciliationTaskEntity task = taskService.checkTaskValidity(taskId);
            ReconciliationRuleEntity rule = ruleService.getByTaskId(taskId);

            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

            env.setParallelism(1);



            // 3. 区分批量/实时任务执行
            if (TaskTypeEnum.BATCH.getCode().equals(task.getType())) {
                // 批量对账（T+1）
                new BatchReconciliationProcessor(
                        env,
                        dataSourceReader,
                        task,
                        rule,
                        t1Date,
                        executionRecordId
                ).process();
            } else {
//                // 实时对账
//                new RealtimeReconciliationProcessor(
//                        env,
//                        dataSourceReader,
//                        task,
//                        rule,
//                        executionRecordId
//                ).process();
            }

            // 4. 执行Flink作业
            env.execute("Reconciliation-Task-" + taskId);
        } catch (Exception e) {
            // 失败时更新执行记录状态
            updateExecutionRecordOnFailure(executionRecordId, e.getMessage());
            throw new RuntimeException("Flink作业执行失败", e);
        }
    }

    /**
     * 作业失败时更新执行记录
     */
    private void updateExecutionRecordOnFailure(Long recordId, String errorMsg) {
        try {
            SpringContextHolder.getBean(ExecutionRecordService.class)
                    .updateStatus(recordId, ExecutionStatusEnum.FAIL.getCode(), errorMsg);
        } catch (Exception ex) {
            // 日志记录，确保不影响主流程
        }
    }
}
