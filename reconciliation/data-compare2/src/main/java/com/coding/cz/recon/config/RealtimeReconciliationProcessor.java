//package com.coding.cz.recon.config;
//
///**
// * @description <>
// * @author: zhouchaoyu
// * @Date: 2025-10-30
// */
//
//import com.coding.cz.recon.entity.ReconciliationRuleEntity;
//import com.coding.cz.recon.entity.ReconciliationTaskEntity;
//import lombok.RequiredArgsConstructor;
//import org.apache.flink.streaming.api.datastream.DataStream;
//import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
//
//import java.util.List;
//
//@RequiredArgsConstructor
//public class RealtimeReconciliationProcessor {
//
//    private final StreamExecutionEnvironment env;
//    private final DataSourceReader dataSourceReader;
//    private final ReconciliationTaskEntity task;
//    private final ReconciliationRuleEntity rule;
//    private final Long executionRecordId;
//
//    public void process() {
//        // 1. 读取源表和目标表实时流（Kafka）
//        DataStream<Record> sourceStream = dataSourceReader.readRealtime(
//                env,
//                task.getSourceDataSource(),
//                rule
//        ).assignTimestampsAndWatermarks(WatermarkStrategyFactory.create()); // 处理乱序
//
//        DataStream<Record> targetStream = dataSourceReader.readRealtime(
//                env,
//                task.getTargetDataSource(),
//                rule
//        ).assignTimestampsAndWatermarks(WatermarkStrategyFactory.create());
//
//        // 2. 广播规则流（动态更新规则）
//        BroadcastStream<ReconciliationRuleEntity> ruleBroadcastStream = new RuleBroadcastStream(env).create();
//
//        // 3. 按连接主键关联比对（带状态管理）
//        List<String> joinFields = rule.getJoinFields();
//        DataStream<DifferenceRecord> differenceStream = sourceStream
//                .connect(targetStream)
//                .keyBy(
//                        source -> source.getJoinKey(joinFields),
//                        target -> target.getJoinKey(joinFields)
//                )
//                .process(new RealtimeCoProcessFunction(ruleBroadcastStream.getBroadcastStateDescriptor()))
//                .name("Realtime-Reconciliation-" + task.getId());
//
//        // 4. 实时处理差异结果
//        differenceStream.addSink(new DifferenceHandler(task.getId()))
//                .name("Realtime-Difference-Sink-" + task.getId());
//    }
//}
