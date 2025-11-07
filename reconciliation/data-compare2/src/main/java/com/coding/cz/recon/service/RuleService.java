package com.coding.cz.recon.service;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import com.coding.cz.recon.repository.ReconciliationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final ReconciliationRuleRepository ruleRepository;

    // 根据任务ID查询规则
    public ReconciliationRuleEntity getByTaskId(Long taskId) {
        return ruleRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("任务未配置规则：" + taskId));
    }
}