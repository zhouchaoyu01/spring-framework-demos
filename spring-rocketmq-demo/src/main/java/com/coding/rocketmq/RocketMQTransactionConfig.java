package com.coding.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-09
 */
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RocketMQ5.x 事务消息专属配置类
 * 核心：创建独立的事务消息RocketMQTemplate，与普通消息模板完全隔离
 */
@Slf4j
@Configuration
public class RocketMQTransactionConfig {

    @Value("${rocketmq.name-server:127.0.0.1:9876}")
    private String nameServer;

    /**
     * 手动定义事务消息 Template Bean
     * Bean 名称为 "orderTxRocketMQTemplate"
     */
    @Bean("orderTxRocketMQTemplate")
    public RocketMQTemplate orderTxRocketMQTemplate(RocketMQMessageConverter rocketMQMessageConverter) {
        RocketMQTemplate template = new RocketMQTemplate();

        // 1. 手动配置 Producer
        TransactionMQProducer producer = new TransactionMQProducer("transaction-producer-group");
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);

        // 2. 将 Producer 设置给 Template
        template.setProducer(producer);

        return template;
    }
}
