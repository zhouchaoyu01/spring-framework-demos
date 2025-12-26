package com.example.coupon.mapper;

import com.example.coupon.entity.CouponDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CouponMapper {

    /**
     * 根据优惠券ID查询优惠券
     */
    CouponDO selectByCouponId(@Param("couponId") String couponId);

    /**
     * 根据优惠券码查询优惠券
     */
    CouponDO selectByCouponCode(@Param("couponCode") String couponCode);

    /**
     * 更新优惠券状态
     */
    int updateStatus(@Param("couponId") String couponId, @Param("status") String status, @Param("updateTime") java.util.Date updateTime);

    /**
     * 查询用户可用优惠券数量
     */
    int countAvailableByUserId(@Param("userId") Long userId);

    /**
     * 插入优惠券
     */
    int insert(CouponDO coupon);
}