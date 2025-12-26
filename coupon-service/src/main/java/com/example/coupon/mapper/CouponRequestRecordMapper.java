package com.example.coupon.mapper;

import com.example.coupon.entity.CouponRequestRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponRequestRecordMapper {

    /**
     * 插入请求记录
     */
    int insert(CouponRequestRecordDO record);

    /**
     * 查询是否存在重复请求
     */
    int countByRequestIdAndCouponIdAndOrderId(
            @Param("requestId") String requestId,
            @Param("couponId") String couponId,
            @Param("orderId") String orderId);
}