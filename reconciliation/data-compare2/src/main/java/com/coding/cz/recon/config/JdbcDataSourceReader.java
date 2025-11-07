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
import com.coding.cz.recon.util.DatePlaceholderResolver;
import com.coding.cz.recon.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;

import org.apache.flink.connector.jdbc.core.datastream.source.JdbcSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;


@Slf4j
@Component
public class JdbcDataSourceReader implements DataSourceReader , Serializable {

    private static final long serialVersionUID = 12L; // 序列化版本号（必填）

    @Override
    public DataStream<DataRecord> readBatch(
            StreamExecutionEnvironment env,
            ReconciliationTaskEntity task,
            DataSourceEntity dataSource, // 仅用于获取连接信息（URL、用户名等）
            LocalDate t1Date,
            ReconciliationRuleEntity rule,
            String type) {
        // 1. 从任务实体中获取当前读取的表名（源表/目标表）
        String tableName = getTableName(task,type);

        // 2. 解析数据源连接参数（从DataSourceEntity获取，JSON格式）
        Map<String, String> connParams = JsonUtils.parseMap(dataSource.getConnectionParams());
        String url = connParams.get("url");
        String username = connParams.get("username");
        String password = connParams.get("password");
        String driver = connParams.getOrDefault("driver", "com.mysql.cj.jdbc.Driver"); // 默认MySQL驱动

        //3. 获取查询字段包括连接字段
        List<String> queryFields = getQueryFields(rule, type);

        // 3. 构建查询SQL（包含表名、过滤条件、日期范围）
        String sql = buildQuerySql(tableName, rule, t1Date, type, queryFields);
        log.info("查询SQL：" + sql);

        // 4. 定义返回字段类型（根据连接主键和比对字段动态生成）
        RowTypeInfo rowTypeInfo = buildRowTypeInfo(rule,type,queryFields);


        // 5. 构建Flink JDBC Source
        JdbcSource<Row> jdbcSource = JdbcSource.<Row>builder()
                .setDriverName(driver)
                .setDBUrl(url)
                .setUsername(username)
                .setPassword(password)
                .setSql(sql)
                .setResultExtractor(resultSet -> {
                    Row row = new Row(queryFields.size()); // 按字段数量初始化Row
                    for (int i = 0; i < queryFields.size(); i++) {
                        String fieldName = queryFields.get(i); // 字段名
                        try {
                            // 从ResultSet中取对应字段的值（支持不同类型）
                            Object value = resultSet.getObject(fieldName);
                            row.setField(i, value); // 按索引设置字段值
//                            log.info("字段[" + fieldName + "]值：" + value);
                        } catch (SQLException e) {
                            throw new RuntimeException("字段[" + fieldName + "]转换失败", e);
                        }

                    }
                    return row;


                })
                .setTypeInformation(TypeInformation.of(Row.class))
                .build();

        log.info("构建Flink JDBC Source完成");
        // 6. 读取数据并转换为DataRecord（Flink内存载体）
        return env.fromSource(
                        jdbcSource,
                        WatermarkStrategy.noWatermarks(), // 批量数据无需水印
                        "JDBC-Batch-" + type + "-" + dataSource.getId()
                )
                .map(row -> convertToDataRecord(row, rowTypeInfo))
                .name("JDBC-To-DataRecord-" + type);
    }

    /**
     * 从任务实体中获取表名（源表/目标表）
     */
    private String getTableName(ReconciliationTaskEntity task,String tableType) {
        if ("source".equals(tableType)) {
            return task.getSourceTable();
        } else if ("target".equals(tableType)) {
            return task.getTargetTable();
        } else {
            throw new IllegalArgumentException("无效的表类型：" + tableType);
        }
    }

    /**
     * 构建查询SQL（包含连接主键、比对字段、过滤条件和日期）
     */
    private String buildQuerySql(String tableName, ReconciliationRuleEntity rule, LocalDate t1Date,
                                 String tableType,List<String> queryFields) {
        // 2. 构建基础SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(String.join(",", queryFields))
                .append(" FROM ").append(tableName)
                .append(" WHERE 1=1");

        // 3. 拼接规则中的过滤条件（如"status = 'SUCCESS'"）
        String filterCondition = "source".equals(tableType)
                ? rule.getSourceFilterConditions()
                : rule.getTargetFilterConditions();
        if (filterCondition != null && !filterCondition.isEmpty()) {
            // 核心：用基准日期t1Date解析占位符（如${T-1}→t1Date的前一天）
            String resolvedCondition = DatePlaceholderResolver.resolve(filterCondition, t1Date);
            sql.append(" AND ").append(resolvedCondition);
        }

        return sql.toString();
    }

    /**
     * 定义返回字段的类型信息（简化版：默认字符串类型）
     */
    private RowTypeInfo buildRowTypeInfo(ReconciliationRuleEntity rule,String tableType,List<String> fieldNames) {

        // 所有字段暂按String类型处理（实际应根据数据库字段类型动态设置）
        TypeInformation<?>[] fieldTypes = new TypeInformation[fieldNames.size()];
        for (int i = 0; i < fieldNames.size(); i++) {
            fieldTypes[i] = TypeInformation.of(String.class);
        }

        return new RowTypeInfo(fieldTypes, fieldNames.toArray(new String[0]));
    }

    /**
     * 将Flink Row转换为DataRecord（内存载体）
     */
    private DataRecord convertToDataRecord(Row row, RowTypeInfo rowTypeInfo) {
        DataRecord record = new DataRecord();
        String[] fieldNames = rowTypeInfo.getFieldNames(); // 从RowTypeInfo获取字段名数组

        for (int i = 0; i < row.getArity(); i++) {
            String fieldName = fieldNames[i]; // 按索引匹配字段名
            Object fieldValue = row.getField(i);
            // 转换为字符串（处理null值）
            String value = fieldValue != null ? fieldValue.toString() : "";
            record.setField(fieldName, value);
        }
//        log.info("DataRecord info: " + print(record.getFields()));
        return record;
    }

    // 提取查询字段生成逻辑为独立方法
    private List<String> getQueryFields(ReconciliationRuleEntity rule, String type) {
        List<String> queryFields = new ArrayList<>();
        // 添加连接字段
        for (ReconciliationRuleEntity.JoinField joinField : rule.getJoinFields()) {
            queryFields.add("source".equals(type) ? joinField.getSourceField() : joinField.getTargetField());
        }
        // 添加比对字段
        for (ReconciliationRuleEntity.CompareField compareField : rule.getCompareFields()) {
            if("source".equals(type) && !queryFields.contains(compareField.getSourceField())){
                queryFields.add(compareField.getSourceField());
            }else if("target".equals(type) && !queryFields.contains(compareField.getTargetField())){
                queryFields.add(compareField.getTargetField());
            }
        }
        return queryFields;
    }
    public static String print(Map<String, String>  fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }
        builder.append("}");
        return builder.toString();
    }
}