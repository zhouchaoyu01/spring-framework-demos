package com.cz.v2.service;

import com.cz.v2.entity.LocalTransaction;
import com.cz.v2.factory.PaymentStrategyProcessFactory;
import com.cz.v2.mapper.LocalTransactionMapper;
import com.cz.v2.template.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-12-17
 */
@Slf4j
@Service
public class PaymentServiceImpl implements IPaymentService{
    @Autowired
    private PaymentStrategyProcessFactory paymentStrategyProcessFactory;
    @Autowired
    private LocalTransactionMapper localTransactionMapper;
    @Override
    public void unitPay(String type, String money) {
        PaymentProcessor paymentStrategy = paymentStrategyProcessFactory.getPaymentStrategy(type);
        paymentStrategy.processPayment(type,Double.parseDouble(money));
        LocalTransaction transaction = new LocalTransaction();
        transaction.setOrderId(new String("P")+System.currentTimeMillis()+(int)(Math.random()*1000));
        transaction.setAmount(new BigDecimal(money));
        transaction.setStatus("SUCCESS");
        localTransactionMapper.insertBatch(Arrays.asList(transaction));

    }
    // 注入优惠券服务
    @Autowired
    private CouponService couponService;
    @Override
    public void unitPayWithCoupon(String type, String money, String couponId) throws Exception {
        BigDecimal originalAmount = new BigDecimal(money);
        BigDecimal actualAmount = originalAmount;

        // 模拟调用外部渠道核销优惠券
        if (couponService.deductCoupon(couponId, originalAmount)) {
            // 优惠券核销成功，计算实际支付金额
            BigDecimal couponAmount = couponService.getCouponAmount(couponId);
            actualAmount = originalAmount.subtract(couponAmount);

            // 确保金额不会小于0
            if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
                actualAmount = BigDecimal.ZERO;
            }
        }else {

            throw new Exception("Invalid coupon");
        }

        // 使用实际金额进行支付
        PaymentProcessor paymentStrategy = paymentStrategyProcessFactory.getPaymentStrategy(type);
        paymentStrategy.processPayment(type, actualAmount.doubleValue());

        // 记录交易信息，包含优惠信息
        LocalTransaction transaction = new LocalTransaction();
        transaction.setOrderId(new String("P")+System.currentTimeMillis()+(int)(Math.random()*1000));
        transaction.setAmount(actualAmount);
        transaction.setStatus("SUCCESS");
        localTransactionMapper.insertBatch(Arrays.asList(transaction));
    }
}
