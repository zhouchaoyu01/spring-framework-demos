package com.coding.cz.recon.util;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    // JSON字符串转Map
    public static Map<String, String> parseMap(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败：" + json, e);
        }
    }
}