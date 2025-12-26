package com.example.couponorder.service;

import com.example.couponorder.client.CouponClient;
import com.example.couponorder.client.CouponRollbackReq;
import com.example.couponorder.client.CouponRollbackResp;
import com.example.couponorder.entity.OrderDO;
import com.example.couponorder.entity.OrderCouponVerifyDO;
import com.example.couponorder.exception.BusinessException;
import com.example.couponorder.mapper.OrderMapper;
import com.example.couponorder.mapper.OrderCouponVerifyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 订单关闭服务
 */
@Slf4j
@Service
public class OrderCloseService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderCouponVerifyMapper verifyMapper;

    @Autowired
    private CouponClient couponClient;

    /**
     * 关单逻辑：冲正优惠券 + 订单改为CLOSED
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(String orderId) {
        // 1. 查询订单和核销记录
        OrderDO order = orderMapper.selectById(orderId);
        if (order == null || !"CREATED".equals(order.getStatus())) {
            log.info("订单{}无需关单：状态非CREATED", orderId);
            return;
        }

        String couponId = order.getCouponId();
        if (StringUtils.isEmpty(couponId)) {
            // 无优惠券，直接改订单为CLOSED
            order.setStatus("CLOSED");
            order.setUpdateTime(new Date());
            orderMapper.updateById(order);
            log.info("订单{}关单成功：无优惠券", orderId);
            return;
        }

        // 2. 调用优惠券冲正接口
        CouponRollbackReq rollbackReq = new CouponRollbackReq();
        rollbackReq.setCouponId(couponId);
        rollbackReq.setOrderId(orderId);
        rollbackReq.setReason("订单关闭，自动冲正");

        CouponRollbackResp resp;
        try {
            resp = couponClient.rollbackCoupon(rollbackReq);
        } catch (Exception e) {
            log.error("订单{}调用优惠券冲正接口失败", orderId, e);
            throw new BusinessException("优惠券冲正失败，关单失败");
        }

        if (!resp.isSuccess()) {
            log.error("订单{}优惠券冲正失败：{}", orderId, resp.getMsg());
            throw new BusinessException("优惠券冲正失败，关单失败");
        }

        // 3. 更新订单为CLOSED + 核销记录为ROLLBACKED
        order.setStatus("CLOSED");
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
        verifyDO.setOrderId(orderId);
        verifyDO.setCouponId(couponId);
        verifyDO.setVerifyStatus("ROLLBACKED");
        verifyDO.setRollbackTime(new Date());
        verifyDO.setUpdateTime(new Date());
        verifyMapper.updateByOrderIdAndCouponId(verifyDO);

        log.info("订单{}关单成功：优惠券已冲正", orderId);
    }

    /**
     * 本地事务：订单改为CLOSED + 核销记录改为ROLLBACKED
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderClosedAndVerifyRollbacked(String orderId, String couponId) {
        // 更新订单为CLOSED
        OrderDO order = new OrderDO();
        order.setOrderId(orderId);
        order.setStatus("CLOSED");
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        // 更新核销记录为ROLLBACKED
        OrderCouponVerifyDO verifyDO = new OrderCouponVerifyDO();
        verifyDO.setOrderId(orderId);
        verifyDO.setCouponId(couponId);
        verifyDO.setVerifyStatus("ROLLBACKED");
        verifyDO.setRollbackTime(new Date());
        verifyDO.setUpdateTime(new Date());
        verifyMapper.updateByOrderIdAndCouponId(verifyDO);
    }
}