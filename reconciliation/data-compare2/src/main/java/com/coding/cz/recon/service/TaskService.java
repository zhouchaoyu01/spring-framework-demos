package com.coding.cz.recon.service;

import com.coding.cz.recon.dto.TaskTriggerDTO;
import com.coding.cz.recon.entity.ReconciliationTaskEntity;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */



public interface TaskService {
    /**
     * 触发对账任务
     */
    void triggerTask(TaskTriggerDTO dto);

    /**
     * 校验任务合法性
     */
    ReconciliationTaskEntity checkTaskValidity(Long taskId);
}
