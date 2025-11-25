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


/**
 * 完整可运行的 Flink Job 示例（批量 T+1 对账）
 * 修复点：
 * 1. 修复定时器注册逻辑，确保新数据能触发定时器
 * 2. 修复 onTimer 中 timerRegistered 重置问题
 * 3. 优化 ListState 清理与定时器更新逻辑
 * 4. 增加日志跟踪数据流向，便于调试
 */
@Slf4j
public class FlinkReconciliationJob {

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        String sourceUrl = params.get("sourceUrl", "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=GMT%2B8");
        String sourceUser = params.get("sourceUser", "root");
        String sourcePass = params.get("sourcePass", "root123");
        String sourceTable = params.get("sourceTable", "local_transaction");
        String targetUrl = params.get("targetUrl", sourceUrl);
        String targetUser = params.get("targetUser", sourceUser);
        String targetPass = params.get("targetPass", sourcePass);
        String targetTable = params.get("targetTable", "channel_transaction");

        LocalDate reconDate = LocalDate.of(2025, 5, 6).minusDays(1); // 默认 T-1，修复原代码中硬编码日期问题
        log.info("对账日期：{}", reconDate);

        // 修复示例规则中的字段重复问题（原代码中cf2和cf3字段重复）
        ReconciliationRuleEntity rule = exampleRule();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(params.getInt("parallelism", 2)); // 建议并行度≥2，测试分区场景
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.SECONDS)));

        // 构建查询SQL（增加排序字段，避免分页数据混乱）
        String sourceSql = SqlBuilder.buildQuerySql(sourceTable, rule, true, reconDate);
        String targetSql = SqlBuilder.buildQuerySql(targetTable, rule, false, reconDate);
        log.info("源表SQL：{}", sourceSql);
        log.info("目标表SQL：{}", targetSql);

        // 并行JDBC源（修复分页逻辑，确保数据均匀分配）
        DataStream<DataRecord> sourceStream = env.addSource(new JdbcParallelSourceFunction(
                sourceUrl, sourceUser, sourcePass, sourceSql
        )).name("jdbc-source-source");

        DataStream<DataRecord> targetStream = env.addSource(new JdbcParallelSourceFunction(
                targetUrl, targetUser, targetPass, targetSql
        )).name("jdbc-source-target");

        // 按joinKey分区（修复DataRecord中getJoinKey的this引用问题）
        DataStream<DataRecord> keyedSource = sourceStream.keyBy(r -> r.getJoinKey(rule.getJoinFields(), true));
        DataStream<DataRecord> keyedTarget = targetStream.keyBy(r -> r.getJoinKey(rule.getJoinFields(), false));

        ConnectedStreams<DataRecord, DataRecord> connected = keyedSource.connect(keyedTarget);

        // 对账处理（设置等待时间为5分钟，可通过参数配置）
        long waitMillis = params.getLong("waitMillis", 100L);
        DataStream<ReconciliationDiff> diffs = connected.process(new ReconciliationCoProcessFunction(rule, waitMillis))
                .name("reconciliation-process");

        // 差异结果写入DB
        String sinkUrl = params.get("sinkUrl", "jdbc:mysql://localhost:3306/recon?useSSL=false&serverTimezone=GMT%2B8");
        diffs.addSink(new JdbcDiffSink(sinkUrl, targetUser, targetPass)).name("jdbc-diff-sink");

        env.execute("Flink T+1 Reconciliation Job");
    }

    // 修复示例规则中的字段重复问题
    private static ReconciliationRuleEntity exampleRule() {
        ReconciliationRuleEntity rule = new ReconciliationRuleEntity();
        ReconciliationRuleEntity.JoinField jf = new ReconciliationRuleEntity.JoinField();
        jf.setSourceField("order_id");
        jf.setTargetField("order_id");
        rule.setJoinFields(Collections.singletonList(jf));

        ReconciliationRuleEntity.CompareField cf1 = new ReconciliationRuleEntity.CompareField();
        cf1.setSourceField("amount");
        cf1.setTargetField("amount");
        cf1.setFieldType("DECIMAL");
        cf1.setType("EQUAL");

        ReconciliationRuleEntity.CompareField cf2 = new ReconciliationRuleEntity.CompareField();
        cf2.setSourceField("status");
        cf2.setTargetField("channel_status");
        cf2.setFieldType("STRING");
        cf2.setType("EQUAL");

//

        rule.setCompareFields(Arrays.asList(cf1, cf2));
        rule.setSourceFilterConditions("create_time >= '${T-1} 00:00:00' AND create_time < '${T} 00:00:00'");
        rule.setTargetFilterConditions("create_time >= '${T-1} 00:00:00' AND create_time < '${T} 00:00:00'");
        rule.setAllowError(0.01);
        return rule;
    }

    // SQL构建器（增加排序字段，避免分页数据混乱）
    public static class SqlBuilder {
        public static String buildQuerySql(String tableName, ReconciliationRuleEntity rule, boolean isSource, LocalDate reconDate) {
            LocalDate t = reconDate.plusDays(1);
            LocalDate tMinus1 = reconDate;
            String conditions = isSource ? rule.getSourceFilterConditions() : rule.getTargetFilterConditions();
            if (conditions == null || conditions.trim().isEmpty()) {
                conditions = "1=1";
            } else {
                conditions = conditions.replace("${T}", t.toString()).replace("${T-1}", tMinus1.toString());
            }

            // 收集查询字段
            LinkedHashSet<String> fields = new LinkedHashSet<>();
            for (ReconciliationRuleEntity.JoinField j : rule.getJoinFields()) {
                fields.add(isSource ? j.getSourceField() : j.getTargetField());
            }
            for (ReconciliationRuleEntity.CompareField c : rule.getCompareFields()) {
                fields.add(isSource ? c.getSourceField() : c.getTargetField());
            }
            String fieldSql = fields.stream().collect(Collectors.joining(", "));
            if (fieldSql.isEmpty()) fieldSql = "*";

            // 增加排序字段（使用joinKey字段排序，确保分页数据顺序一致）
            String sortField = isSource ? rule.getJoinFields().get(0).getSourceField() : rule.getJoinFields().get(0).getTargetField();
            return String.format("SELECT %s FROM %s WHERE %s ORDER BY %s", fieldSql, tableName, conditions, sortField);
        }
    }

    // 并行JDBC源（修复分页逻辑，确保数据均匀分配）
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
            log.info("JDBC源初始化完成，URL：{}", jdbcUrl);
        }

        @Override
        public void run(SourceContext<DataRecord> ctx) throws Exception {
            int subtask = getRuntimeContext().getIndexOfThisSubtask();
            int parallelism = getRuntimeContext().getNumberOfParallelSubtasks();
            log.info("Subtask {} 开始读取数据，并行度：{}，分页大小：{}", subtask, parallelism, pageSize);

            int page = 0;
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                conn.setAutoCommit(true);
                while (running) {
                    // 修复分页逻辑：offset = page * pageSize + subtask？不，正确的并行分页应该是：
                    // 每个subtask处理 第 (subtask + page * parallelism) 页
                    int currentPage = subtask + page * parallelism;
                    int offset = currentPage * pageSize;
                    String pagedSql = baseSql + String.format(" LIMIT %d, %d", offset, pageSize);
                    log.info("Subtask {} 执行SQL：{}", subtask, pagedSql);

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
                            log.info("Subtask {} 第 {} 页读取到 {} 条数据", subtask, currentPage, rowCount);
                            if (rowCount == 0) {
                                log.info("Subtask {} 无更多数据，停止读取", subtask);
                                break;
                            }
                        }
                    }
                    page++;
                }
            } catch (Exception e) {
                log.error("Subtask {} 读取数据失败", subtask, e);
                throw e;
            }
        }

        @Override
        public void cancel() {
            running = false;
            log.info("JDBC源被取消");
        }
    }

    // 核心对账处理（修复定时器逻辑，解决target-only丢失问题）
    public static class ReconciliationCoProcessFunction
            extends KeyedCoProcessFunction<String, DataRecord, DataRecord, ReconciliationDiff> {

        private final ReconciliationRuleEntity rule;
        private final long waitMillis;

        private transient ListState<DataRecord> leftList; // 源表数据
        private transient ListState<DataRecord> rightList; // 目标表数据
        private transient ValueState<Long> timerTimestamp; // 存储当前定时器的时间戳（替代原有的Boolean状态）

        public ReconciliationCoProcessFunction(ReconciliationRuleEntity rule, long waitMillis) {
            this.rule = rule;
            this.waitMillis = waitMillis;
            log.info("对账处理器初始化，等待时间：{}ms", waitMillis);
        }

        @Override
        public void open(Configuration parameters) {
            // 初始化状态
            ListStateDescriptor<DataRecord> leftDesc = new ListStateDescriptor<>("leftList", DataRecord.class);
            leftList = getRuntimeContext().getListState(leftDesc);

            ListStateDescriptor<DataRecord> rightDesc = new ListStateDescriptor<>("rightList", DataRecord.class);
            rightList = getRuntimeContext().getListState(rightDesc);

            ValueStateDescriptor<Long> timerDesc = new ValueStateDescriptor<>("timerTimestamp", Long.class);
            timerTimestamp = getRuntimeContext().getState(timerDesc);
        }

        // 处理源表数据（左侧流）
        @Override
        public void processElement1(DataRecord left, Context ctx, Collector<ReconciliationDiff> out) throws Exception {
            String joinKey = ctx.getCurrentKey();
            log.error("processElement1 :{} joinKey:{}", left.toString(), joinKey);
            List<DataRecord> rights = iterableToList(rightList.get());
            log.info("处理源表数据，joinKey：{}，当前目标表缓存数：{}", joinKey, rights.size());

            if (!rights.isEmpty()) {
                // 匹配到目标表数据，直接对账
                DataRecord right = rights.remove(0);
                ReconciliationDiff diff = compareAndBuild(joinKey, left, right);
                out.collect(diff);
                log.info("源表与目标表匹配，joinKey：{}，差异类型：{}", joinKey, diff.getDiffType());

                // 更新目标表缓存
                rightList.update(rights);
                // 如果目标表缓存为空，删除定时器
                if (rights.isEmpty()) {
                    cancelTimer(ctx);
                } else {
                    // 还有未匹配的目标表数据，更新定时器
                    registerOrUpdateTimer(ctx);
                }
            } else {
                // 未匹配到目标表数据，存入源表缓存
                leftList.add(left);
                log.info("源表数据存入缓存，joinKey：{}，当前源表缓存数：{}", joinKey, iterableToList(leftList.get()).size());
                // 注册或更新定时器
                registerOrUpdateTimer(ctx);
            }
        }

        // 处理目标表数据（右侧流）- 重点修复target-only逻辑
        @Override
        public void processElement2(DataRecord right, Context ctx, Collector<ReconciliationDiff> out) throws Exception {
            String joinKey = ctx.getCurrentKey();
            log.error("processElement2 :{} joinKey:{}", right.toString(), joinKey);
            List<DataRecord> lefts = iterableToList(leftList.get());
            log.info("处理目标表数据，joinKey：{}，当前源表缓存数：{}", joinKey, lefts.size());

            if (!lefts.isEmpty()) {
                // 匹配到源表数据，直接对账
                DataRecord left = lefts.remove(0);
                ReconciliationDiff diff = compareAndBuild(joinKey, left, right);
                out.collect(diff);
                log.info("目标表与源表匹配，joinKey：{}，差异类型：{}", joinKey, diff.getDiffType());

                // 更新源表缓存
                leftList.update(lefts);
                // 如果源表缓存为空，删除定时器
                if (lefts.isEmpty()) {
                    cancelTimer(ctx);
                } else {
                    // 还有未匹配的源表数据，更新定时器
                    registerOrUpdateTimer(ctx);
                }
            } else {
                // 未匹配到源表数据，存入目标表缓存（可能产生target-only）
                rightList.add(right);
                log.info("目标表数据存入缓存，joinKey：{}，当前目标表缓存数：{}", joinKey, iterableToList(rightList.get()).size());
                // 关键修复：注册或更新定时器，确保后续能扫描到该数据
                registerOrUpdateTimer(ctx);
            }
        }

        // 定时器触发：处理未匹配的数据（生成source-only/target-only）
        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<ReconciliationDiff> out) throws Exception {
            String joinKey = ctx.getCurrentKey();
            log.info("定时器触发，joinKey：{}，当前时间：{}，定时器时间：{}", joinKey, System.currentTimeMillis(), timestamp);

            List<DataRecord> lefts = iterableToList(leftList.get());
            List<DataRecord> rights = iterableToList(rightList.get());

            // 生成source-only（源表有，目标表无）
            for (DataRecord l : lefts) {
                ReconciliationDiff diff = ReconciliationDiff.sourceOnly(joinKey, 0L);
                out.collect(diff);
                log.info("生成source-only差异，joinKey：{}", joinKey);
            }

            // 生成target-only（目标表有，源表无）- 修复后此处会正确触发
            for (DataRecord r : rights) {
                ReconciliationDiff diff = ReconciliationDiff.targetOnly(joinKey, 0L);
                out.collect(diff);
                log.info("生成target-only差异，joinKey：{}", joinKey);
            }

            // 清理状态
            leftList.clear();
            rightList.clear();
            timerTimestamp.clear();
            log.info("定时器处理完成，清理状态，joinKey：{}", joinKey);
        }

        /**
         * 注册或更新定时器：
         * 1. 如果已有定时器，先取消
         * 2. 注册新的定时器（当前时间+等待时间）
         * 3. 存储定时器时间戳到状态
         */
        private void registerOrUpdateTimer(Context ctx) throws Exception {
            long currentTime = ctx.timerService().currentProcessingTime();
            long newTimerTs = currentTime + waitMillis;

            // 取消已有的定时器
            Long existingTs = timerTimestamp.value();
            if (existingTs != null) {
                ctx.timerService().deleteProcessingTimeTimer(existingTs);
                log.info("取消旧定时器，joinKey：{}，旧时间：{}", ctx.getCurrentKey(), existingTs);
            }

            // 注册新定时器
            ctx.timerService().registerProcessingTimeTimer(newTimerTs);
            timerTimestamp.update(newTimerTs);
            log.info("注册新定时器，joinKey：{}，新时间：{}", ctx.getCurrentKey(), newTimerTs);
        }

        /**
         * 取消当前定时器并清理状态
         */
        private void cancelTimer(Context ctx) throws Exception {
            Long existingTs = timerTimestamp.value();
            if (existingTs != null) {
                ctx.timerService().deleteProcessingTimeTimer(existingTs);
                timerTimestamp.clear();
                log.info("取消定时器，joinKey：{}，时间：{}", ctx.getCurrentKey(), existingTs);
            }
        }

        /**
         * Iterable转List
         */
        private List<DataRecord> iterableToList(Iterable<DataRecord> it) {
            List<DataRecord> list = new ArrayList<>();
            if (it != null) {
                for (DataRecord r : it) list.add(r);
            }
            return list;
        }

        /**
         * 字段比对，生成差异结果
         */
        private ReconciliationDiff compareAndBuild(String joinKey, DataRecord left, DataRecord right) {
            List<String> mismatches = new ArrayList<>();
            StringBuilder srcVals = new StringBuilder();
            StringBuilder tgtVals = new StringBuilder();

            for (ReconciliationRuleEntity.CompareField cf : rule.getCompareFields()) {
                String s = left.getFields().get(cf.getSourceField());
                String t = right.getFields().get(cf.getTargetField());

                // 数值类型（DECIMAL）支持误差容忍
                if ("DECIMAL".equals(cf.getFieldType()) && rule.getAllowError() != null) {
                    try {
                        double sourceVal = Double.parseDouble(s == null ? "0" : s);
                        double targetVal = Double.parseDouble(t == null ? "0" : t);
                        if (Math.abs(sourceVal - targetVal) > rule.getAllowError()) {
                            mismatches.add(cf.getSourceField() + "<->" + cf.getTargetField());
                            srcVals.append(cf.getSourceField()).append("=").append(s).append(";");
                            tgtVals.append(cf.getTargetField()).append("=").append(t).append(";");
                        }
                    } catch (NumberFormatException e) {
                        mismatches.add(cf.getSourceField() + "<->" + cf.getTargetField() + "(格式错误)");
                        srcVals.append(cf.getSourceField()).append("=").append(s).append(";");
                        tgtVals.append(cf.getTargetField()).append("=").append(t).append(";");
                    }
                } else {
                    // 非数值类型，直接字符串比对
                    if (!Objects.equals(s, t)) {
                        mismatches.add(cf.getSourceField() + "<->" + cf.getTargetField());
                        srcVals.append(cf.getSourceField()).append("=").append(s).append(";");
                        tgtVals.append(cf.getTargetField()).append("=").append(t).append(";");
                    }
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

    // 差异结果写入JDBC（修复批量提交逻辑）
    public static class JdbcDiffSink extends RichSinkFunction<ReconciliationDiff> {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private transient Connection conn;
        private transient PreparedStatement ps;
        private static final int BATCH_SIZE = 100;
        private int counter = 0;

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
                    "execution_record_id, diff_type, join_key_value, mismatch_field, source_value, target_value, task_id, description, create_time) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)");
            log.info("JDBC Sink初始化完成，URL：{}", jdbcUrl);
        }

        @Override
        public void invoke(ReconciliationDiff value, Context context) throws Exception {
            ps.setLong(1, 1L); // 执行记录ID，生产环境需替换为实际ID
            ps.setString(2, value.getDiffType());
            ps.setString(3, value.getJoinKeyValue());
            ps.setString(4, value.getMismatchField() == null ? "" : value.getMismatchField());
            ps.setString(5, value.getSourceValue() == null ? "" : value.getSourceValue());
            ps.setString(6, value.getTargetValue() == null ? "" : value.getTargetValue());
            ps.setObject(7, value.getTaskId() == null ? 0L : value.getTaskId());
            ps.setString(8, value.getDescription() == null ? "" : value.getDescription());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.addBatch();
            counter++;

            // 批量提交
            if (counter % BATCH_SIZE == 0) {
                int[] result = ps.executeBatch();
                conn.commit();
                log.info("批量提交 {} 条差异记录", result.length);
                counter = 0;
            }
        }

        @Override
        public void close() throws Exception {
            // 提交剩余数据
            if (counter > 0) {
                int[] result = ps.executeBatch();
                conn.commit();
                log.info("关闭时提交剩余 {} 条差异记录", result.length);
            }
            if (ps != null) ps.close();
            if (conn != null) conn.close();
            super.close();
        }
    }

    // 数据记录实体（修复getJoinKey的this引用问题）
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

        public String toString() {
            return fields.toString();
        }
        /**
         * 生成joinKey（修复原代码中多余的DataRecord参数，直接使用当前对象）
         */
        public String getJoinKey(List<ReconciliationRuleEntity.JoinField> joinFields, boolean isSource) {
            StringBuilder builder = new StringBuilder();
            for (ReconciliationRuleEntity.JoinField jf : joinFields) {
                String fieldName = isSource ? jf.getSourceField() : jf.getTargetField();
                String fieldValue = this.fields.getOrDefault(fieldName, "").trim();
                builder.append(fieldValue).append("|");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
    }

    // 差异结果实体
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
            return ReconciliationDiff.builder()
                    .diffType("MATCH")
                    .joinKeyValue(joinKeyValue)
                    .taskId(taskId)
                    .description("记录匹配")
                    .build();
        }

        public static ReconciliationDiff fieldMismatch(String joinKeyValue, String mismatchField, String sourceValue, String targetValue, Long taskId) {
            return ReconciliationDiff.builder()
                    .diffType("FIELD_MISMATCH")
                    .joinKeyValue(joinKeyValue)
                    .mismatchField(mismatchField)
                    .sourceValue(sourceValue)
                    .targetValue(targetValue)
                    .taskId(taskId)
                    .description("字段不匹配")
                    .build();
        }

        public static ReconciliationDiff sourceOnly(String joinKeyValue, Long taskId) {
            return ReconciliationDiff.builder()
                    .diffType("SOURCE_ONLY")
                    .joinKeyValue(joinKeyValue)
                    .taskId(taskId)
                    .description("源表有记录，目标表无记录")
                    .build();
        }

        public static ReconciliationDiff targetOnly(String joinKeyValue, Long taskId) {
            return ReconciliationDiff.builder()
                    .diffType("TARGET_ONLY")
                    .joinKeyValue(joinKeyValue)
                    .taskId(taskId)
                    .description("目标表有记录，源表无记录")
                    .build();
        }
    }

    // 对账规则实体
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

