package com.coding.cz.recon.service.flink.processor;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-11-12
 */


import com.coding.cz.recon.config.*;
import com.coding.cz.recon.dto.DataRecord;
import com.coding.cz.recon.entity.DataSourceEntity;
import com.coding.cz.recon.dto.ReconciliationDiff;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.Serializable;
import java.time.LocalDate;

import java.util.List;


/**
 * 基于双流Join的对账处理器
 * 替代原有的coGroup方案，更适合T+1全量数据比对场景
 */
@Slf4j
@RequiredArgsConstructor
public class JoinBasedReconciliationProcessor implements Serializable {

    private final StreamExecutionEnvironment env;
    private final DataSourceEntity sourceDataSource;
    private final DataSourceEntity targetDataSource;
    private final ReconciliationTaskEntity task;
    private final ReconciliationRuleEntity rule;
    private final LocalDate reconDate;
    private final Long executionRecordId;

    public void process() {
        try {
            // 1. 读取源表和目标表数据
            DataStream<DataRecord> sourceStream = readDataSourceStream(sourceDataSource, "source", task);
            DataStream<DataRecord> targetStream = readDataSourceStream(targetDataSource, "target", task);


            // 2. 获取连接条件
            List<ReconciliationRuleEntity.JoinField> joinFields = rule.getJoinFields();
            log.info("连接条件：{}", joinFields);
            if (joinFields == null || joinFields.isEmpty()) {
                throw new RuntimeException("对账规则未配置连接条件（joinFields）");
            }

            // 3. 为源表和目标表数据添加水印（批量数据无需水印，使用无水印策略）
            DataStream<DataRecord> sourceWithWatermark = sourceStream.assignTimestampsAndWatermarks(
                    WatermarkStrategy.noWatermarks()
            );
            DataStream<DataRecord> targetWithWatermark = targetStream.assignTimestampsAndWatermarks(
                    WatermarkStrategy.noWatermarks()
            );

            // 4. 双流Join比对（主连接，处理匹配的数据）
            DataStream<ReconciliationDiff> joinResultStream = sourceWithWatermark
                    .join(targetWithWatermark)
                    .where(record -> record.getJoinKey(joinFields, true))
                    .equalTo(record -> record.getJoinKey(joinFields, false))
                    .window(TumblingProcessingTimeWindows.of(Time.days(1)))

                    .apply(new ReconciliationJoinFunction(task, rule), TypeInformation.of(new TypeHint<ReconciliationDiff>() {
                    }));

            joinResultStream.print("666match666");

            /*
            // 5. 处理单边数据（源表有目标表无，或反之）
            // 5.1 处理源表单边数据
            DataStream<ReconciliationDiff> sourceOnlyStream = processSourceOnlyData(sourceStream, targetStream, joinFields);
            // 5.2 处理目标表单边数据
            DataStream<ReconciliationDiff> targetOnlyStream = processTargetOnlyData(sourceStream, targetStream, joinFields);

            // 6. 合并所有差异结果（Join比对结果 + 源表单边 + 目标表单边）
            DataStream<ReconciliationDiff> allDiffStream = joinResultStream
                    .union(sourceOnlyStream)
                    .union(targetOnlyStream);
                    */

            // 7. 差异结果落地（可复用原有的DifferenceHandler）
            joinResultStream.addSink(new ReconciliationDiffSink(executionRecordId))
                    .name("Reconciliation-Diff-Sink-" + task.getId());

            log.info("基于Join的对账流程构建完成，任务ID: {}", task.getId());



        } catch (Exception e) {
            log.error("构建基于Join的对账流程失败", e);
            throw new RuntimeException("构建对账流程失败", e);
        }
    }

    /**
     * 读取数据源数据
     */
//    private DataStream<DataRecord> readDataSourceStream(DataSourceEntity dataSource, String type) {
//        DataSourceReader reader = new JdbcDataSourceReader(task, type);
//        return reader.readBatch(env, dataSource, rule, reconDate)
//                .name("JDBC-Reader-" + type + "-" + dataSource.getId());
//    }
    private DataStream<DataRecord> readDataSourceStream(DataSourceEntity dataSource, String type, ReconciliationTaskEntity task) {
        DataSourceReader dataSourceReader = new JdbcDataSourceReader();
        return dataSourceReader.readBatch(env, task, dataSource, reconDate, rule, type)
                ;
    }

    /**
     * 处理源表单边数据（源表有记录，目标表无记录）
     */
    private DataStream<ReconciliationDiff> processSourceOnlyData(
            DataStream<DataRecord> sourceStream,
            DataStream<DataRecord> targetStream,
            List<ReconciliationRuleEntity.JoinField> joinFields) {

        // 将目标表数据转换为连接键流
        DataStream<String> targetJoinKeys = targetStream
                .map(record -> record.getJoinKey(joinFields, false))
                .name("Target-Join-Key-Extract");

        // 使用coGroup处理单边数据（这里仍然使用coGroup处理单边场景）
        return sourceStream
                .coGroup(targetJoinKeys)
                .where(record -> record.getJoinKey(joinFields, true))
                .equalTo(key -> key)
                .window(TumblingProcessingTimeWindows.of(Time.days(1)))
                .apply(new SourceCoGroupFunction(task, joinFields));
    }


    /**
     * 处理目标表单边数据（目标表有记录，源表无记录）
     */
    private DataStream<ReconciliationDiff> processTargetOnlyData(
            DataStream<DataRecord> sourceStream,
            DataStream<DataRecord> targetStream,
            List<ReconciliationRuleEntity.JoinField> joinFields) {

        // 将源表数据转换为连接键流
        DataStream<String> sourceJoinKeys = sourceStream
                .map(record -> record.getJoinKey(joinFields, true))
                .name("Source-Join-Key-Extract");

        // 使用coGroup处理单边数据
        return targetStream
                .coGroup(sourceJoinKeys)
                .where(record -> record.getJoinKey(joinFields, false))
                .equalTo(key -> key)
                .window(TumblingProcessingTimeWindows.of(Time.days(1)))
                .apply(new TargetCoGroupFunction(task, joinFields));
    }
}

