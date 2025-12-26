package com.example.couponorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用程序主类
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class CouponOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponOrderApplication.class, args);
    }
}