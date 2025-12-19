package com.cz.v2.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author zhouchaoyu
 * @time 2024-08-18-22:53
 */
@Slf4j
@Service("wechat")

public class WechatProcessor extends PaymentProcessor{
    @Override
    public void processPayment(String type, double amount) {
        log.info("WeChatPayStrategy pay... Using Wechat to pay " + amount + " yuan." );
        // 微信支付逻辑
        // 支付宝支付逻辑
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
    }
}
