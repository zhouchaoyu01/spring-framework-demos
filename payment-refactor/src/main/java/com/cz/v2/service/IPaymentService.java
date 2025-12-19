package com.cz.v2.service;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-12-17
 */
public interface IPaymentService {
    void unitPay(String type, String amount);
    // 添加带优惠券的支付方法
    void unitPayWithCoupon(String type, String amount, String couponId) throws Exception;

}
