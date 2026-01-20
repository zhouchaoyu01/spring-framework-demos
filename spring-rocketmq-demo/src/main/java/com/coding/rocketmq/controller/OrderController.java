package com.coding.rocketmq.controller;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-08
 */

import com.coding.rocketmq.OrderMessage;
import com.coding.rocketmq.producer.OrderProducerService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducerService orderProducerService;

    @PostMapping("/sync")
    public SendResult createOrderSync(@RequestBody OrderMessage orderMessage) {
        // 设置订单信息
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("CREATED");

        return orderProducerService.sendSyncMessage(orderMessage);
    }

    @PostMapping("/async")
    public String createOrderAsync(@RequestBody OrderMessage orderMessage) {
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("CREATED");

        orderProducerService.sendAsyncMessage(orderMessage);
        return "异步消息已发送";
    }

    @PostMapping("/delay")
    public SendResult createDelayOrder(@RequestBody OrderMessage orderMessage,
                                       @RequestParam(defaultValue = "3") int delayLevel) {
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("DELAY_CREATED");

        return orderProducerService.sendDelayMessage(orderMessage, delayLevel);
    }

    @PostMapping("/orderly")
    public SendResult createOrderlyOrder(@RequestBody OrderMessage orderMessage) {
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("ORDERLY_CREATED");

        // 使用用户ID作为分片键，确保同一用户的消息顺序消费
        return orderProducerService.sendOrderlyMessage(orderMessage, orderMessage.getUserId());
    }

    @PostMapping("/transaction")
    public String createTransactionOrder(@RequestBody OrderMessage orderMessage) {
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("TRANSACTION_CREATED");

        orderProducerService.sendTransactionMessage(orderMessage);
        return "事务消息已发送";
    }

    @GetMapping("/test")
    public OrderMessage testOrder() {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setOrderId(UUID.randomUUID().toString());
        orderMessage.setUserId("user123");
        orderMessage.setProductId("product456");
        orderMessage.setQuantity(2);
        orderMessage.setAmount(new BigDecimal("199.99"));
        orderMessage.setCreateTime(LocalDateTime.now());
        orderMessage.setStatus("TEST");

        return orderMessage;
    }
}