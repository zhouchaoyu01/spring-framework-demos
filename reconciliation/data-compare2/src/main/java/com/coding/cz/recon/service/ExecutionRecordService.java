package com.coding.cz.recon.service;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */



import com.coding.cz.recon.constant.ExecutionStatusEnum;
import com.coding.cz.recon.entity.ExecutionRecordEntity;
import com.coding.cz.recon.repository.ExecutionRecordRepository;
import com.coding.cz.recon.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ExecutionRecordService {

    private final ExecutionRecordRepository executionRecordRepository;

    // 创建执行记录
    public ExecutionRecordEntity createRecord(Long taskId, LocalDate t1Date) {
        ExecutionRecordEntity record = new ExecutionRecordEntity();
        record.setExecutionNo(IdGenerator.generateExecutionNo()); // 生成唯一编号
        record.setTaskId(taskId);
        record.setType(2); // 2-定时调度（默认）
        record.setStatus(ExecutionStatusEnum.PROCESSING.getCode()); // 处理中
        record.setStartTime(LocalDateTime.now());
        return executionRecordRepository.save(record);
    }

    // 更新执行记录状态
    public void updateStatus(Long recordId, Integer status, String errorMsg) {
        ExecutionRecordEntity record = executionRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在：" + recordId));

        record.setStatus(status);
        record.setEndTime(LocalDateTime.now());
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        LocalDateTime endTime = record.getEndTime();
        LocalDateTime startTime = record.getStartTime();
        long e = endTime.atZone(shanghaiZone).toInstant().toEpochMilli();
        long s = startTime.atZone(shanghaiZone).toInstant().toEpochMilli();
        record.setCostMs(e-s);
        if (errorMsg != null) {
            record.setErrorMsg(errorMsg);
        }
        executionRecordRepository.save(record);
    }

    // 更新统计结果
    public void updateStatistics(Long recordId, Long totalCount, Long matchCount, Long differenceCount) {
        ExecutionRecordEntity record = executionRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在：" + recordId));

        record.setTotalCount(totalCount);
        record.setMatchCount(matchCount);
        record.setDifferenceCount(differenceCount);
        executionRecordRepository.save(record);
    }
}