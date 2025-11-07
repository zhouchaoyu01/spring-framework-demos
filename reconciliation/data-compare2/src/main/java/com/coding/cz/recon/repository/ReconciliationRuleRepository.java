package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.ReconciliationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReconciliationRuleRepository extends JpaRepository<ReconciliationRuleEntity, Long> {
    Optional<ReconciliationRuleEntity> findByTaskId(Long taskId);
}