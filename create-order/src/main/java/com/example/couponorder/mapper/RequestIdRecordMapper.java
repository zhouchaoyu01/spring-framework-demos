package com.example.couponorder.mapper;

import com.example.couponorder.entity.RequestIdRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RequestIdRecordMapper {

    /**
     * 插入请求记录
     */
    int insert(RequestIdRecordDO record);

    /**
     * 查询是否存在重复请求
     */
    int countByRequestIdAndUserIdAndBizType(
            @Param("requestId") String requestId,
            @Param("userId") Long userId,
            @Param("bizType") String bizType);
}