package com.example.couponorder.client.fallback;

import com.example.couponorder.client.CouponClient;
import com.example.couponorder.client.CouponRollbackReq;
import com.example.couponorder.client.CouponRollbackResp;
import com.example.couponorder.client.CouponVerifyReq;
import com.example.couponorder.client.CouponVerifyResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券服务降级处理
 */
@Slf4j
@Component
public class CouponClientFallback implements CouponClient {

    @Override
    public CouponVerifyResp verifyCoupon(CouponVerifyReq req) {
        log.error("优惠券核销服务降级，couponId: {}, orderId: {}", req.getCouponId(), req.getOrderId());
        CouponVerifyResp resp = new CouponVerifyResp();
        resp.setSuccess(false);
        resp.setMsg("优惠券服务暂时不可用，请稍后重试");
        return resp;
    }

    @Override
    public CouponRollbackResp rollbackCoupon(CouponRollbackReq req) {
        log.error("优惠券冲正服务降级，couponId: {}, orderId: {}", req.getCouponId(), req.getOrderId());
        CouponRollbackResp resp = new CouponRollbackResp();
        resp.setSuccess(false);
        resp.setMsg("优惠券服务暂时不可用，请稍后重试");
        return resp;
    }
}