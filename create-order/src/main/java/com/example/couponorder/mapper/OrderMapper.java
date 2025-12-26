package com.example.couponorder.mapper;

import com.example.couponorder.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    int insert(OrderDO order);

    /**
     * 根据ID更新订单
     */
    int updateById(OrderDO order);

    /**
     * 根据ID查询订单
     */
    OrderDO selectById(@Param("orderId") String orderId);

    /**
     * 根据用户ID和业务类型查询是否存在重复订单
     */
    int countByUserIdAndBizType(@Param("userId") Long userId, @Param("bizType") String bizType);
}