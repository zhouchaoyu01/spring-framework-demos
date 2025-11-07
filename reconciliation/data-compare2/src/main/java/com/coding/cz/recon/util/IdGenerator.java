package com.coding.cz.recon.util;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class IdGenerator {
    // 生成执行记录编号（如E202510300001）
    public static String generateExecutionNo() {
        return "E" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    // 生成差异记录编号（如D202510300001）
    public static String generateDifferenceNo() {
        return "D" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
