package com.example.couponorder.controller;

import com.example.couponorder.service.OrderCreateService;
import com.example.couponorder.service.OrderCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderCreateService orderCreateService;

    @Autowired
    private OrderCloseService orderCloseService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<String> createOrder(@RequestBody OrderCreateService.OrderCreateReq req) {
        try {
            log.info("收到创建订单请求：userId={}, couponId={}", req.getUserId(), req.getCouponId());
            String orderId = orderCreateService.createOrder(req);
            return Result.success(orderId);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 关闭订单
     */
    @PostMapping("/close/{orderId}")
    public Result<Boolean> closeOrder(@PathVariable String orderId) {
        try {
            log.info("收到关闭订单请求：orderId={}", orderId);
            orderCloseService.closeOrder(orderId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("关闭订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 通用响应对象
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public Result(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(200, "success", data);
        }

        public static <T> Result<T> error(String message) {
            return new Result<>(500, message, null);
        }

        // Getters and Setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}