package com.coding;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-04
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * RocketMQ Spring Boot Demo 启动类
 *
 * 使用说明：
 * 1. 确保本地已启动 RocketMQ NameServer（默认 127.0.0.1:9876）
 * 2. 确保已创建相应的 Topic
 * 3. 启动应用
 * 4. 通过 REST API 测试消息发送
 */
@SpringBootApplication
public class RocketMQApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocketMQApplication.class, args);
    }

}