package com.coding.cz.recon.controller;

import com.coding.cz.recon.service.FlinkGatewayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-21
 */
@RestController
@RequestMapping("/flink")
public class FlinkController {

    @Autowired
    private FlinkGatewayClient flinkGatewayClient;
    @RequestMapping("/executeSql")
    public String executeSql(String sessionId, String sql) {
        return flinkGatewayClient.executeSql(sessionId, sql).block();
    }
}
