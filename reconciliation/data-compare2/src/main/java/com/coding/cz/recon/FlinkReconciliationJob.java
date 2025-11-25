package com.coding.cz.recon;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-11-24
 */


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.*;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Collector;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 完整可运行的 Flink Job 示例（批量 T+1 对账）
 * <p>
 * - 包含并行 JDBC Source（基于 LIMIT 分页）
 * - 使用 KeyedCoProcessFunction 在 Key 上进行双流匹配与字段比对
 * - 将差异写回 JDBC（批量提交）
 * <p>
 * 说明：为便于在你的工程中快速集成，代码尽量使用你提供的 DataRecord、ReconciliationDiff 概念。
 * 在生产中请替换为你的真实实体加载（例如从 DB 加载 ReconciliationRuleEntity、DataSourceEntity 等）。
 *
 *
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
@Slf4j
public class FlinkReconciliationJob {

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        // 示例参数：--sourceUrl jdbc:mysql://localhost:3306/test_db --sourceUser root --sourcePass root123
        String sourceUrl = params.get("sourceUrl", "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=GMT%2B8");
        String sourceUser = params.get("sourceUser", "root");
        String sourcePass = params.get("sourcePass", "root123");
        String sourceTable = params.get("sourceTable", "local_transaction");
        String targetUrl = params.get("targetUrl", sourceUrl);
        String targetUser = params.get("targetUser", sourceUser);
        String targetPass = params.get("targetPass", sourcePass);
        String targetTable = params.get("targetTable", "channel_transaction");

        long reconDateEpoch = Long.parseLong(params.get("reconDateEpoch", String.valueOf(LocalDate.now().toEpochDay())));
        LocalDate reconDate = LocalDate.of(2025,5,9).minusDays(1); // 默认 T-1

        // Example rule: join on order_id; compare amount and status
        ReconciliationRuleEntity rule = exampleRule();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(params.getInt("parallelism", 1));
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // 尝试重启 3 次
                Time.of(10, TimeUnit.SECONDS) // 每次间隔 10 秒
        ));
        // Build SQLs
        String sourceSql = SqlBuilder.buildQuerySql(sourceTable, rule, true, reconDate);
        String targetSql = SqlBuilder.buildQuerySql(targetTable, rule, false, reconDate);
        log.info("SQLs: source={}, target={}", sourceSql, targetSql);
        // Create parallel JDBC sources
        DataStream<DataRecord> sourceStream = env.addSource(new JdbcParallelSourceFunction(
                sourceUrl, sourceUser, sourcePass, sourceSql
        )).name("jdbc-source-source");

        DataStream<DataRecord> targetStream = env.addSource(new JdbcParallelSourceFunction(
                targetUrl, targetUser, targetPass, targetSql
        )).name("jdbc-source-target");

        // Key by join key (stringified composite key)
        DataStream<DataRecord> keyedSource = sourceStream.keyBy(r -> r.getJoinKey(r,rule.getJoinFields(), true));
        DataStream<DataRecord> keyedTarget = targetStream.keyBy(r -> r.getJoinKey(r,rule.getJoinFields(), false));

        ConnectedStreams<DataRecord, DataRecord> connected = keyedSource.connect(keyedTarget);

        DataStream<ReconciliationDiff> diffs = connected.process(new ReconciliationCoProcessFunction(rule, 30 * 60 * 1000L));

        // Sink diffs back to DB
        String SINKUrl = params.get("sourceUrl", "jdbc:mysql://localhost:3306/recon?useSSL=false&serverTimezone=GMT%2B8");

        diffs.addSink(new JdbcDiffSink(SINKUrl, targetUser, targetPass)).name("jdbc-diff-sink");

        env.execute("Flink T+1 Reconciliation Job");
    }

    // -------------------- Example Rule --------------------
    private static ReconciliationRuleEntity exampleRule() {
        ReconciliationRuleEntity rule = new ReconciliationRuleEntity();
        ReconciliationRuleEntity.JoinField jf = new ReconciliationRuleEntity.JoinField();
        jf.setSourceField("order_id");
        jf.setTargetField("order_id");
        rule.setJoinFields(Collections.singletonList(jf));

        ReconciliationRuleEntity.CompareField cf1 = new ReconciliationRuleEntity.CompareField();
        cf1.setSourceField("amount"); cf1.setTargetField("amount"); cf1.setFieldType("DECIMAL"); cf1.setType("EQUAL");
        ReconciliationRuleEntity.CompareField cf2 = new ReconciliationRuleEntity.CompareField();
        cf2.setSourceField("status"); cf2.setTargetField("channel_status"); cf2.setFieldType("STRING"); cf2.setType("EQUAL");
//        ReconciliationRuleEntity.CompareField cf3 = new ReconciliationRuleEntity.CompareField();
//        cf2.setSourceField("status"); cf3.setTargetField("channel_status"); cf3.setFieldType("STRING"); cf2.setType("EQUAL");
        rule.setCompareFields(Arrays.asList(cf1, cf2));

        // Filter example using T-1 placeholder
        rule.setSourceFilterConditions("create_time >= '${T-1} 00:00:00' AND create_time < '${T} 00:00:00'");
        rule.setTargetFilterConditions("create_time >= '${T-1} 00:00:00' AND create_time < '${T} 00:00:00'");
        rule.setAllowError(0.01);
        return rule;
    }

    // -------------------- SqlBuilder --------------------
    public static class SqlBuilder {
        public static String buildQuerySql(String tableName, ReconciliationRuleEntity rule, boolean isSource, LocalDate reconDate) {
            LocalDate t = reconDate.plusDays(1); // if reconDate passed as T-1, adjust accordingly in caller
            LocalDate tMinus1 = reconDate;
            String conditions = isSource ? rule.getSourceFilterConditions() : rule.getTargetFilterConditions();
            if (conditions == null || conditions.trim().isEmpty()) {
                conditions = "1=1";
            } else {
                conditions = conditions.replace("${T}", t.toString()).replace("${T-1}", tMinus1.toString());
            }
            LinkedHashSet<String> fields = new LinkedHashSet<>();
            for (ReconciliationRuleEntity.JoinField j : rule.getJoinFields()) {
                fields.add(isSource ? j.getSourceField() : j.getTargetField());
            }
            for (ReconciliationRuleEntity.CompareField c : rule.getCompareFields()) {
                fields.add(isSource ? c.getSourceField() : c.getTargetField());
            }
            String fieldSql = fields.stream().collect(Collectors.joining(", "));
            if (fieldSql.isEmpty()) fieldSql = "*";
            return String.format("SELECT %s FROM %s WHERE %s", fieldSql, tableName, conditions);
        }
    }

    // -------------------- Parallel JDBC Source --------------------
    public static class JdbcParallelSourceFunction extends RichParallelSourceFunction<DataRecord> {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private final String baseSql;
        private volatile boolean running = true;
        private final int pageSize = 10000;

        public JdbcParallelSourceFunction(String jdbcUrl, String username, String password, String baseSql) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.baseSql = baseSql;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            Class.forName("com.mysql.cj.jdbc.Driver");
        }

        @Override
        public void run(SourceContext<DataRecord> ctx) throws Exception {
            int subtask = getRuntimeContext().getIndexOfThisSubtask();
            int parallelism = getRuntimeContext().getNumberOfParallelSubtasks();

            int page = 0;
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                conn.setAutoCommit(true);
                while (running) {
                    int offset = (page * parallelism + subtask) * pageSize;
                    String pagedSql = baseSql + String.format(" LIMIT %d, %d", offset, pageSize);
                    try (PreparedStatement ps = conn.prepareStatement(pagedSql)) {
                        ps.setQueryTimeout(60);
                        try (ResultSet rs = ps.executeQuery()) {
                            int rowCount = 0;
                            ResultSetMetaData meta = rs.getMetaData();
                            while (rs.next()) {
                                Map<String, String> map = new HashMap<>();
                                for (int i = 1; i <= meta.getColumnCount(); i++) {
                                    String col = meta.getColumnLabel(i);
                                    Object obj = rs.getObject(i);
                                    map.put(col, obj == null ? null : obj.toString());
                                }
                                DataRecord dr = new DataRecord(map);
                                synchronized (ctx.getCheckpointLock()) {
                                    ctx.collect(dr);
                                }
                                rowCount++;
                            }
                            if (rowCount == 0) {
                                // no more pages for this partition
                                break;
                            }
                        }
                    }
                    page++;
                }
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }

    // -------------------- CoProcessFunction --------------------
    public static class ReconciliationCoProcessFunction
            extends KeyedCoProcessFunction<String, DataRecord, DataRecord, ReconciliationDiff> {

        private final ReconciliationRuleEntity rule;
        private final long waitMillis;

        private transient ListState<DataRecord> leftList;
        private transient ListState<DataRecord> rightList;
        private transient ValueState<Boolean> timerRegistered;

        public ReconciliationCoProcessFunction(ReconciliationRuleEntity rule, long waitMillis) {
            this.rule = rule;
            this.waitMillis = waitMillis;
        }

        @Override
        public void open(Configuration parameters) {
            leftList = getRuntimeContext().getListState(
                    new ListStateDescriptor<>("leftList", DataRecord.class));

            rightList = getRuntimeContext().getListState(
                    new ListStateDescriptor<>("rightList", DataRecord.class));

            timerRegistered = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("timerRegistered", Boolean.class));
        }

        @Override
        public void processElement1(DataRecord left, Context ctx, Collector<ReconciliationDiff> out) throws Exception {
            List<DataRecord> rights = iterableToList(rightList.get());

            if (!rights.isEmpty()) {
                // 取一个 pair 出来比较
                DataRecord right = rights.remove(0);
                out.collect(compareAndBuild(ctx.getCurrentKey(), left, right));

                rightList.update(rights);
            } else {
                leftList.add(left);
                registerOnce(ctx);
            }
        }

        @Override
        public void processElement2(DataRecord right, Context ctx, Collector<ReconciliationDiff> out) throws Exception {

            List<DataRecord> lefts = iterableToList(leftList.get());

            if (!lefts.isEmpty()) {
                DataRecord left = lefts.remove(0);
                out.collect(compareAndBuild(ctx.getCurrentKey(), left, right));

                leftList.update(lefts);
            } else {
                rightList.add(right);
                registerOnce(ctx);
            }
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<ReconciliationDiff> out) throws Exception {
            String key = ctx.getCurrentKey();

            List<DataRecord> lefts = iterableToList(leftList.get());
            List<DataRecord> rights = iterableToList(rightList.get());

            // ① left-only
            for (DataRecord l : lefts) {
                out.collect(ReconciliationDiff.sourceOnly(key, 0L));
            }

            // ② right-only
            for (DataRecord r : rights) {
                out.collect(ReconciliationDiff.targetOnly(key, 0L));
            }

            leftList.clear();
            rightList.clear();
            timerRegistered.clear();
        }

        private void registerOnce(Context ctx) throws Exception {
            Boolean reg = timerRegistered.value();
            if (reg == null || !reg) {
                long ts = ctx.timerService().currentProcessingTime() + waitMillis;
                ctx.timerService().registerProcessingTimeTimer(ts);
                timerRegistered.update(true);
            }
        }

        private List<DataRecord> iterableToList(Iterable<DataRecord> it) {
            List<DataRecord> list = new ArrayList<>();
            if (it != null) {
                for (DataRecord r : it) list.add(r);
            }
            return list;
        }

        private ReconciliationDiff compareAndBuild(String joinKey, DataRecord left, DataRecord right) {
            List<String> mismatches = new ArrayList<>();
            StringBuilder srcVals = new StringBuilder();
            StringBuilder tgtVals = new StringBuilder();

            for (ReconciliationRuleEntity.CompareField cf : rule.getCompareFields()) {
                String s = left.getFields().get(cf.getSourceField());
                String t = right.getFields().get(cf.getTargetField());
                if (!Objects.equals(s, t)) {
                    mismatches.add(cf.getSourceField());
                    srcVals.append(cf.getSourceField()).append("=").append(s).append(";");
                    tgtVals.append(cf.getTargetField()).append("=").append(t).append(";");
                }
            }

            if (mismatches.isEmpty()) {
                return ReconciliationDiff.match(joinKey, 0L);
            } else {
                return ReconciliationDiff.fieldMismatch(
                        joinKey,
                        String.join(",", mismatches),
                        srcVals.toString(),
                        tgtVals.toString(),
                        0L
                );
            }
        }
    }


    // -------------------- Sink --------------------
    public static class JdbcDiffSink extends RichSinkFunction<ReconciliationDiff> {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private transient Connection conn;
        private transient PreparedStatement ps;

        public JdbcDiffSink(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            conn.setAutoCommit(false);
            ps = conn.prepareStatement("INSERT INTO rdc_reconciliation_difference_record(" +
                    "execution_record_id, diff_type, join_key_value, mismatch_field, source_value, target_value, task_id, description, create_time) VALUES(?,?,?,?,?,?,?,?,?)");
        }

        @Override
        public void invoke(ReconciliationDiff value, Context context) throws Exception {
            ps.setLong(1, 1L);
            ps.setString(2, value.getDiffType());
            ps.setString(3, value.getJoinKeyValue());
            ps.setString(4, value.getMismatchField());
            ps.setString(5, value.getSourceValue());
            ps.setString(6, value.getTargetValue());
            ps.setObject(7, value.getTaskId());
            ps.setString(8, value.getDescription());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.addBatch();
            // 简单批量策略：定时提交或按条提交（此处按每条提交以保证可见性）
            ps.executeBatch();
            conn.commit();
        }

        @Override
        public void close() throws Exception {
            if (ps != null) ps.close();
            if (conn != null) conn.close();
            super.close();
        }
    }

    // -------------------- Simple Domain Classes --------------------

    public static class DataRecord implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, String> fields = new HashMap<>();

        public DataRecord() {
        }

        public DataRecord(Map<String, String> map) {
            if (map != null) this.fields.putAll(map);
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public String getJoinKey(DataRecord r,List<ReconciliationRuleEntity.JoinField> joinFields, boolean isSource) {
            StringBuilder builder = new StringBuilder();
            for (ReconciliationRuleEntity.JoinField jf : joinFields) {
                String field = isSource ? jf.getSourceField() : jf.getTargetField();
                String val = r.getFields().getOrDefault(field, "");
                builder.append(val.trim()).append("|");
            }
            builder.delete(builder.length() - 1, builder.length());
            return builder.toString();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationDiff implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private String diffType;
        private String joinKeyValue;
        private String mismatchField;
        private String sourceValue;
        private String targetValue;
        private Long taskId;
        private String description;

        public static ReconciliationDiff match(String joinKeyValue, Long taskId) {
            return ReconciliationDiff.builder().diffType("MATCH").joinKeyValue(joinKeyValue).taskId(taskId).description("记录匹配").build();
        }

        public static ReconciliationDiff fieldMismatch(String joinKeyValue, String mismatchField, String sourceValue, String targetValue, Long taskId) {
            return ReconciliationDiff.builder().diffType("FIELD_MISMATCH").joinKeyValue(joinKeyValue).mismatchField(mismatchField)
                    .sourceValue(sourceValue).targetValue(targetValue).taskId(taskId).description("字段不匹配").build();
        }

        public static ReconciliationDiff sourceOnly(String joinKeyValue, Long taskId) {
            return ReconciliationDiff.builder().diffType("SOURCE_ONLY").joinKeyValue(joinKeyValue).taskId(taskId).description("源表有记录，目标表无记录").build();
        }

        public static ReconciliationDiff targetOnly(String joinKeyValue, Long taskId) {
            return ReconciliationDiff.builder().diffType("TARGET_ONLY").joinKeyValue(joinKeyValue).taskId(taskId).description("目标表有记录，源表无记录").build();
        }
    }

    @Data
    public static class ReconciliationRuleEntity implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private List<JoinField> joinFields = new ArrayList<>();
        private List<CompareField> compareFields = new ArrayList<>();
        private String sourceFilterConditions;
        private String targetFilterConditions;
        private Double allowError;

        @Data
        public static class CompareField implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
            private String sourceField;
            private String targetField;
            private String type;
            private String fieldType;
            private String allowError;
        }

        @Data
        public static class JoinField implements java.io.Serializable {
            private static final long serialVersionUID = 1L;
            private String sourceField;
            private String targetField;
        }
    }
}

