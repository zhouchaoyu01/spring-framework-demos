//package com.example.couponorder.service;
//
//import com.example.couponorder.client.CouponClient;
//import com.example.couponorder.client.CouponRollbackReq;
//import com.example.couponorder.client.CouponRollbackResp;
//import com.example.couponorder.entity.OrderDO;
//import com.example.couponorder.entity.OrderCouponVerifyDO;
//import com.example.couponorder.mapper.OrderMapper;
//import com.example.couponorder.mapper.OrderCouponVerifyMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
///**
// * 订单补偿定时任务
// */
//@Slf4j
//@Component
//public class OrderCompensateTask {
//
//    @Autowired
//    private OrderMapper orderMapper;
//
//    @Autowired
//    private OrderCouponVerifyMapper verifyMapper;
//
//    @Autowired
//    private CouponClient couponClient;
//
//    @Autowired
//    private RedissonClient redissonClient;
//
//    @Autowired
//    private OrderCloseService orderCloseService;
//
//    /**
//     * 定时补偿异常订单（每分钟执行一次）
//     */
//    @Scheduled(fixedRate = 60 * 1000)
//    public void compensateAbnormalOrder() {
//        log.info("开始执行订单补偿任务");
//
//        try {
//            // 1. 查询异常记录：核销成功（SUCCESS）但订单非CREATED
//            List<OrderCouponVerifyDO> abnormalList = verifyMapper.selectByVerifyStatusAndOrderNotCreated("SUCCESS");
//            log.info("发现{}条异常订单记录需要补偿", abnormalList.size());
//
//            for (OrderCouponVerifyDO verifyDO : abnormalList) {
//                String orderId = verifyDO.getOrderId();
//                String couponId = verifyDO.getCouponId();
//
//                // 2. 分布式锁防止重复补偿
//                RLock lock = redissonClient.getLock("lock:order:compensate:" + orderId);
//                if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
//                    log.warn("订单{}补偿锁被占用，跳过本次补偿", orderId);
//                    continue;
//                }
//
//                try {
//                    // 3. 先重试更新订单为CREATED
//                    boolean updateOrderSuccess = false;
//                    try {
//                        OrderDO order = new OrderDO();
//                        order.setOrderId(orderId);
//                        order.setStatus("CREATED");
//                        order.setUpdateTime(new Date());
//                        int updateCount = orderMapper.updateById(order);
//                        updateOrderSuccess = updateCount == 1;
//                        if (updateOrderSuccess) {
//                            log.info("订单{}补偿成功：重试更新为CREATED成功", orderId);
//                        }
//                    } catch (Exception e) {
//                        log.error("订单{}重试更新CREATED失败", orderId, e);
//                    }
//
//                    // 4. 订单更新失败 → 冲正优惠券 + 订单改为CLOSED
//                    if (!updateOrderSuccess) {
//                        log.warn("订单{}更新失败，尝试冲正优惠券并关闭订单", orderId);
//
//                        CouponRollbackReq rollbackReq = new CouponRollbackReq();
//                        rollbackReq.setCouponId(couponId);
//                        rollbackReq.setOrderId(orderId);
//                        rollbackReq.setReason("订单创建异常，自动冲正优惠券");
//
//                        CouponRollbackResp resp = couponClient.rollbackCoupon(rollbackReq);
//                        if (resp.isSuccess()) {
//                            // 本地事务：订单改为CLOSED + 核销记录改为ROLLBACKED
//                            orderCloseService.updateOrderClosedAndVerifyRollbacked(orderId, couponId);
//                            log.info("订单{}补偿完成：冲正优惠券并关闭订单", orderId);
//                        } else {
//                            log.error("订单{}冲正优惠券失败，下次任务重试", orderId);
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("订单{}补偿异常", orderId, e);
//                } finally {
//                    if (lock.isHeldByCurrentThread()) {
//                        lock.unlock();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("订单补偿任务执行异常", e);
//        }
//
//        log.info("订单补偿任务执行完成");
//    }
//}