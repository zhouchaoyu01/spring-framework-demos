package com.coding.cz.recon.util;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析过滤条件中的日期占位符（如${T-1}）
 */
public class DatePlaceholderResolver {
    // 占位符正则：匹配${xxx}格式
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    // 日期格式（yyyy-MM-dd）
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析带占位符的过滤条件，替换为实际日期
     * @param condition 带占位符的过滤条件（如"trans_time >= '${T-1} 00:00:00'"）
     * @param baseDate 基准日期（通常为对账触发时的日期，如T日）
     * @return 替换后的过滤条件
     */
    public static String resolve(String condition, LocalDate baseDate) {
        if (condition == null || condition.isEmpty()) {
            return "";
        }

        // 生成占位符映射（占位符→实际日期）
        Map<String, String> placeholderMap = buildPlaceholderMap(baseDate);

        // 替换占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(condition);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholderKey = matcher.group(1); // 提取${}中的内容（如"T-1"）
            String replacement = placeholderMap.getOrDefault(placeholderKey, matcher.group()); // 未匹配的占位符保留原样
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 构建占位符与实际日期的映射关系
     */
    private static Map<String, String> buildPlaceholderMap(LocalDate baseDate) {
        Map<String, String> map = new HashMap<>();
        // 当天（T）
        map.put("T", baseDate.format(DATE_FORMATTER));
        // T-1日
        map.put("T-1", baseDate.minusDays(1).format(DATE_FORMATTER));
        // T-2日
        map.put("T-2", baseDate.minusDays(2).format(DATE_FORMATTER));
        // 当月第一天
        map.put("CURRENT_MONTH_FIRST", baseDate.withDayOfMonth(1).format(DATE_FORMATTER));
        // 上月最后一天
        map.put("LAST_MONTH_LAST", baseDate.minusMonths(1).withDayOfMonth(
                baseDate.minusMonths(1).lengthOfMonth()
        ).format(DATE_FORMATTER));
        return map;
    }
}
