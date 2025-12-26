package com.example.couponorder.service;

import com.example.couponorder.entity.RequestIdRecordDO;
import com.example.couponorder.mapper.RequestIdRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 请求防重服务
 */
@Slf4j
@Service
public class RepeatSubmitGuard {

    @Autowired
    private RequestIdRecordMapper requestIdRecordMapper;

    /**
     * 验证请求是否重复
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean validateRequestId(String requestId, Long userId, String bizType) {
        try {
            // 检查是否已存在该请求
            int count = requestIdRecordMapper.countByRequestIdAndUserIdAndBizType(requestId, userId, bizType);
            if (count > 0) {
                log.warn("重复请求：requestId={}, userId={}, bizType={}", requestId, userId, bizType);
                return false;
            }

            // 插入请求记录
            RequestIdRecordDO record = new RequestIdRecordDO();
            record.setRequestId(requestId);
            record.setUserId(userId);
            record.setBizType(bizType);
            record.setCreateTime(new Date());
            requestIdRecordMapper.insert(record);
            return true;
        } catch (Exception e) {
            log.error("防重校验异常：requestId={}, userId={}, bizType={}", requestId, userId, bizType, e);
            // 数据库唯一约束冲突时，认为是重复请求
            return false;
        }
    }

    /**
     * 验证请求是否重复（默认业务类型）
     */
    public boolean validateRequestId(String requestId, Long userId) {
        return validateRequestId(requestId, userId, "ORDER_CREATE");
    }
}