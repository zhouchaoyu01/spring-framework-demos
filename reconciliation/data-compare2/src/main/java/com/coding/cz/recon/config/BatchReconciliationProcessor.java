package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */



import com.coding.cz.recon.dto.DataRecord;
import com.coding.cz.recon.dto.ReconciliationDiff;
import com.coding.cz.recon.entity.DataSourceEntity;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;
import com.coding.cz.recon.repository.DataSourceRepository;
import com.coding.cz.recon.service.TaskService;
import com.coding.cz.recon.util.SpringContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BatchReconciliationProcessor {

    private final StreamExecutionEnvironment env;
    private final DataSourceReader dataSourceReader;
    private final ReconciliationTaskEntity task;
    private final ReconciliationRuleEntity rule;
    private final LocalDate t1Date;
    private final Long executionRecordId;

    public void process() {
        DataSourceRepository dataSourceRepository = SpringContextHolder.getBean(DataSourceRepository.class);
        DataSourceEntity dataSource = dataSourceRepository.findById(task.getSourceDataSourceId()).orElseThrow(() -> new RuntimeException("源数据源不存在"));

        // 读取源表和目标表数据（返回DataStream<DataRecord>）
        DataStream<DataRecord> sourceStream = readDataSourceStream(dataSource, "source", task);
        DataStream<DataRecord> targetStream = readDataSourceStream(dataSource, "target", task);


        // 连接主键校验
        List<ReconciliationRuleEntity.JoinField> joinFields = rule.getJoinFields();
        if (joinFields.isEmpty()) {
            throw new RuntimeException("任务" + task.getId() + "未配置连接主键");
        }

        // 关联比对产生差异（ReconciliationDiff）
        DataStream<ReconciliationDiff> diffStream = sourceStream
                .coGroup(targetStream)
                // 源表：按joinFields中的sourceField生成连接键
                .where(record -> record.getJoinKey(joinFields, true))
                // 目标表：按joinFields中的targetField生成连接键
                .equalTo(record -> record.getJoinKey(joinFields, false))
                .window(TumblingProcessingTimeWindows.of(Time.seconds(60)))
                .apply(new BatchCoGroupFunction(rule, task.getId(),joinFields))
               ;

        // 差异落地
        diffStream.addSink(new DifferenceHandler(task.getId()))
                .name("Difference-Sink-" + task.getId());

        // 统计结果
//        new ExecutionStatisticHandler(
//                sourceStream,
//                targetStream,
//                diffStream,
//                executionRecordId
//        ).statistic();
    }

    private DataStream<DataRecord> readDataSourceStream(DataSourceEntity dataSource, String type, ReconciliationTaskEntity task) {
        return dataSourceReader.readBatch(env,task, dataSource, t1Date, rule, type)
                ;
    }
}