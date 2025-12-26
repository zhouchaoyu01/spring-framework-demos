package com.example.couponorder.mapper;

import com.example.couponorder.entity.OrderCouponVerifyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderCouponVerifyMapper {

    /**
     * 插入核销记录
     */
    int insert(OrderCouponVerifyDO verifyDO);

    /**
     * 根据订单ID和优惠券ID更新核销记录
     */
    int updateByOrderIdAndCouponId(OrderCouponVerifyDO verifyDO);

    /**
     * 根据订单ID查询核销记录
     */
    OrderCouponVerifyDO selectByOrderId(@Param("orderId") String orderId);

    /**
     * 查询核销成功但订单未创建的异常记录
     */
    List<OrderCouponVerifyDO> selectByVerifyStatusAndOrderNotCreated(@Param("verifyStatus") String verifyStatus);

    /**
     * 根据唯一键查询核销记录
     */
    OrderCouponVerifyDO selectByUniqueKey(@Param("uniqueKey") String uniqueKey);
}