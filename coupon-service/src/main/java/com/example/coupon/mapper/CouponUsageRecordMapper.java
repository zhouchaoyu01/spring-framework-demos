package com.example.coupon.mapper;

import com.example.coupon.entity.CouponUsageRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponUsageRecordMapper {

    /**
     * 插入使用记录
     */
    int insert(CouponUsageRecordDO record);

    /**
     * 根据优惠券ID和订单ID查询使用记录
     */
    CouponUsageRecordDO selectByCouponIdAndOrderId(@Param("couponId") String couponId, @Param("orderId") String orderId);

    /**
     * 查询优惠券的核销记录
     */
    CouponUsageRecordDO selectVerifyRecord(@Param("couponId") String couponId, @Param("orderId") String orderId);

    /**
     * 查询优惠券的冲正记录
     */
    CouponUsageRecordDO selectRollbackRecord(@Param("couponId") String couponId, @Param("orderId") String orderId);
}