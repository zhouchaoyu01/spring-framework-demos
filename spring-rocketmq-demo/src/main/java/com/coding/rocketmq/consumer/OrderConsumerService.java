package com.coding.rocketmq.consumer;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-08
 */

import com.alibaba.fastjson.JSONObject;
import com.coding.rocketmq.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class OrderConsumerService {

    // 普通消息消费者
    @Service
    @RocketMQMessageListener(
            topic = "ORDER_TOPIC",
            selectorExpression = "ORDER_TAG",
            consumerGroup = "order-consumer-group"
    )
    public class OrderMessageConsumer implements RocketMQListener<OrderMessage> {
        @Override
        public void onMessage(OrderMessage orderMessage) {
            log.info("收到订单消息: {}", orderMessage);
            // 处理订单逻辑
            processOrder(orderMessage);
        }

        private void processOrder(OrderMessage orderMessage) {
            // 业务处理逻辑
            log.info("处理订单: {}", orderMessage.getOrderId());
        }
    }

    // 延迟消息消费者
    @Service
    @RocketMQMessageListener(
            topic = "ORDER_TOPIC",
            selectorExpression = "DELAY_TAG",
            consumerGroup = "delay-order-consumer-group"
    )
    public class DelayOrderConsumer implements RocketMQListener<OrderMessage> {
        @Override
        public void onMessage(OrderMessage orderMessage) {
            log.info("收到延迟订单消息: {}", orderMessage);
            // 处理延迟订单逻辑
        }
    }

    // 顺序消息消费者
    @Service
    @RocketMQMessageListener(
            topic = "ORDER_TOPIC",
            selectorExpression = "ORDERLY_TAG",
            consumerGroup = "orderly-order-consumer-group",
            consumeMode = ConsumeMode.ORDERLY
    )
    public class OrderlyOrderConsumer implements RocketMQListener<OrderMessage> {
        @Override
        public void onMessage(OrderMessage orderMessage) {
            log.info("收到顺序订单消息: {}", orderMessage);
            // 处理顺序订单逻辑
        }
    }

    // 事务消息消费者
    @Service
    @RocketMQTransactionListener(rocketMQTemplateBeanName = "orderTxRocketMQTemplate")
    public class TransactionOrderConsumer implements RocketMQLocalTransactionListener {

        @Override
        public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            try {
                // 1. 打印类型，验证你的猜想
                String jsonBody;
                Object payload = msg.getPayload();
                if (payload instanceof String) {
                    jsonBody = (String) payload;
                } else {
                    jsonBody = new String((byte[]) payload, StandardCharsets.UTF_8);
                }
                log.info("执行本地事务，消息内容: " + jsonBody);
                OrderMessage orderArg = (OrderMessage) arg;
                log.info("执行本地事务，使用 arg 参数: {}", orderArg);
                // 执行本地业务逻辑
                boolean success = processLocalBusiness(jsonBody, arg);

                if (success) {
                    log.info("本地事务执行成功，提交消息");
                    return RocketMQLocalTransactionState.COMMIT;
                } else {
                    log.info("本地事务执行失败，回滚消息");
                    return RocketMQLocalTransactionState.ROLLBACK;
                }

            } catch (Exception e) {
                System.err.println("本地事务执行异常: " + e.getMessage());
                return RocketMQLocalTransactionState.UNKNOWN;
            }
        }

        @Override
        public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
            try {
                // 【修改点】回查时，arg 是 null，必须从 msg body 解析
                // 这里的 payload 可能是 String 也可能是 byte[]，Spring RocketMQ 通常处理为 String (因为发送的是String)
                String jsonBody;
                Object payload = msg.getPayload();

                if (payload instanceof String) {
                    jsonBody = (String) payload;
                } else {
                    jsonBody = new String((byte[]) payload, StandardCharsets.UTF_8);
                }
                OrderMessage messageBody = JSONObject.parseObject(jsonBody, OrderMessage.class);
                // 检查本地事务状态
                boolean completed = checkLocalBusinessStatus(messageBody);

                if (completed) {
                    log.info("本地事务已完成，提交消息");
                    return RocketMQLocalTransactionState.COMMIT;
                } else {
                    log.info("本地事务未完成，回滚消息");
                    return RocketMQLocalTransactionState.ROLLBACK;
                }

            } catch (Exception e) {
                System.err.println("回查本地事务异常: " + e.getMessage());
                return RocketMQLocalTransactionState.UNKNOWN;
            }
        }
        private boolean processLocalBusiness(Object messageBody, Object arg) {
            // 实现你的本地业务逻辑
            // 例如：保存订单到数据库
            log.info("处理本地业务，参数: " + arg);
            return true; // 返回业务执行结果
        }

        private boolean checkLocalBusinessStatus(OrderMessage messageBody) {
            // 检查本地业务状态
            log.info("检查本地业务状态，参数: " + messageBody);
            // 例如：查询订单是否创建成功
            return true; // 返回业务状态
        }
    }
}