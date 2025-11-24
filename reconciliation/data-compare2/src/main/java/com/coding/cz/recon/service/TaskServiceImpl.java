//package com.coding.cz.recon.service;
//
///**
// * @description <>
// * @author: zhouchaoyu
// * @Date: 2025-10-30
// */
//
//
//
//import com.coding.cz.recon.config.FlinkJobManager;
//import com.coding.cz.recon.constant.TaskStatusEnum;
//import com.coding.cz.recon.dto.TaskTriggerDTO;
//import com.coding.cz.recon.entity.ExecutionRecordEntity;
//import com.coding.cz.recon.entity.ReconciliationTaskEntity;
//import com.coding.cz.recon.repository.ExecutionRecordRepository;
//import com.coding.cz.recon.repository.ReconciliationTaskRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class TaskServiceImpl implements TaskService {
//
//    private final ReconciliationTaskRepository taskRepository;
//    private final ExecutionRecordService executionRecordService;
//    private final FlinkJobManager flinkJobManager;
//
//    @Override
//    @Transactional
//    public void triggerTask(TaskTriggerDTO dto) {
//        // 1. 校验任务合法性
//        ReconciliationTaskEntity task = checkTaskValidity(dto.getTaskId());
//
//        // 2. 创建执行记录（初始状态：处理中）
//        ExecutionRecordEntity record = executionRecordService.createRecord(
//                task.getId(),
//                dto.getT1Date()
//        );
//
//        // 3. 提交Flink作业
//        flinkJobManager.submitJob(
//                task.getId(),
//                dto.getT1Date(),
//                record.getId()
//        );
//    }
//
//    @Override
//    public ReconciliationTaskEntity checkTaskValidity(Long taskId) {
//        ReconciliationTaskEntity task = taskRepository.findById(taskId)
//                .orElseThrow(() -> new RuntimeException("任务不存在：" + taskId));
//
//        // 校验任务状态（必须为运行中）
//        if (!TaskStatusEnum.ACTIVE.getCode().equals(task.getStatus())) {
//            throw new RuntimeException("任务未启用：" + taskId);
//        }
//        return task;
//    }
//}
