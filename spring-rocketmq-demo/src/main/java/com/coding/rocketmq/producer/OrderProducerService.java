package com.coding.rocketmq.producer;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-08
 */

import com.alibaba.fastjson.JSON;
import com.coding.rocketmq.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderProducerService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 同步发送消息
    public SendResult sendSyncMessage(OrderMessage orderMessage) {
        Message<OrderMessage> message = MessageBuilder.withPayload(orderMessage).build();
        SendResult sendResult = rocketMQTemplate.syncSend("ORDER_TOPIC:ORDER_TAG", message);
        log.info("同步发送消息成功，消息ID: {}", sendResult.getMsgId());
        return sendResult;
    }

    // 异步发送消息
    public void sendAsyncMessage(OrderMessage orderMessage) {
        Message<OrderMessage> message = MessageBuilder.withPayload(orderMessage).build();
        rocketMQTemplate.asyncSend("ORDER_TOPIC:ORDER_TAG", message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("异步发送消息成功，消息ID: {}", sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("异步发送消息失败", throwable);
            }
        });
    }

    // 发送延迟消息
    public SendResult sendDelayMessage(OrderMessage orderMessage, int delayLevel) {
        Message<OrderMessage> message = MessageBuilder.withPayload(orderMessage).build();
        SendResult sendResult = rocketMQTemplate.syncSend("ORDER_TOPIC:DELAY_TAG", message, 3000, delayLevel);
        log.info("延迟消息发送成功，消息ID: {}", sendResult.getMsgId());
        return sendResult;
    }

    // 发送顺序消息
    public SendResult sendOrderlyMessage(OrderMessage orderMessage, String shardingKey) {
        Message<OrderMessage> message = MessageBuilder.withPayload(orderMessage).build();
        SendResult sendResult = rocketMQTemplate.syncSendOrderly("ORDER_TOPIC:ORDERLY_TAG", message, shardingKey);
        log.info("顺序消息发送成功，消息ID: {}", sendResult.getMsgId());
        return sendResult;
    }
    @Autowired
    @Qualifier("orderTxRocketMQTemplate")
    private RocketMQTemplate transactionRocketMQTemplate;
    // 发送事务消息
    public void sendTransactionMessage(OrderMessage orderMessage) {
        String jsonString = JSON.toJSONString(orderMessage);
        Message<String> message = MessageBuilder.withPayload(jsonString).build();
        TransactionSendResult transactionSendResult = transactionRocketMQTemplate.sendMessageInTransaction("ORDER_TOPIC:TRANSACTION_TAG", message, orderMessage);
        log.info("事务消息发送成功，消息status: {}", transactionSendResult.getSendStatus());
    }
}