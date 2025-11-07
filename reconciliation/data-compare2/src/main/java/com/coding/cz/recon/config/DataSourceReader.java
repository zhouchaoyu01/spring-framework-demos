package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.dto.DataRecord;
import com.coding.cz.recon.entity.DataSourceEntity;
import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.LocalDate;

/**
 * 数据源读取器接口（可扩展实现多类型数据源）
 */
public interface DataSourceReader {

    /**
     * 读取批量数据（T+1对账）
     */
    DataStream<DataRecord> readBatch(
            StreamExecutionEnvironment env,
            ReconciliationTaskEntity task,
            DataSourceEntity dataSource,
            LocalDate t1Date,
            ReconciliationRuleEntity rule,
            String type);

    /**
     * 读取实时流数据（实时对账）
     */
//    DataStream<Record> readRealtime(
//            StreamExecutionEnvironment env,
//            DataSourceEntity dataSource,
//            ReconciliationRuleEntity rule
//    );
}