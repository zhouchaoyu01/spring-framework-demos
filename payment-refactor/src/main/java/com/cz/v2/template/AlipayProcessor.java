package com.cz.v2.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author zhouchaoyu
 * @time 2024-08-18-22:53
 */
@Slf4j
@Service("alipay")
public class AlipayProcessor extends PaymentProcessor{
    @Override
    public void processPayment(String type, double amount) {
        log.info("AlipayStrategy pay... Using Alipay to pay " + amount + " yuan." );
        // 支付宝支付逻辑
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
    }
}
