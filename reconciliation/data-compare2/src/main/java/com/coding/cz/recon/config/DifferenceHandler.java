package com.coding.cz.recon.config;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import com.coding.cz.recon.dto.ReconciliationDiff;
import com.coding.cz.recon.entity.DifferenceRecordEntity;
import com.coding.cz.recon.repository.DifferenceRecordRepository;
import com.coding.cz.recon.util.IdGenerator;
import com.coding.cz.recon.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * 将Flink中的差异信息（ReconciliationDiff）转换为数据库实体（DifferenceRecordEntity）并保存
 */
@Slf4j
public class DifferenceHandler extends RichSinkFunction<ReconciliationDiff> {

    private final Long taskId;
    private transient DifferenceRecordRepository differenceRepository;

    public DifferenceHandler(Long taskId) {
        this.taskId = taskId;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        differenceRepository = SpringContextHolder.getBean(DifferenceRecordRepository.class);
    }

    @Override
    public void invoke(ReconciliationDiff diff, Context context) {

        // 转换为数据库实体（DifferenceRecordEntity）
        DifferenceRecordEntity entity = new DifferenceRecordEntity();
        entity.setDifferenceNo(IdGenerator.generateDifferenceNo());
        entity.setTaskId(taskId);
        entity.setType(diff.getType());
        entity.setSourceDataId(diff.getJoinKeyValue());
        entity.setTargetDataId(diff.getJoinKeyValue());
        entity.setMismatchField(diff.getMismatchField());
        entity.setSourceValue(diff.getSourceValue());
        entity.setTargetValue(diff.getTargetValue());
        entity.setStatus(1); // 待处理
        differenceRepository.save(entity);
    }
}