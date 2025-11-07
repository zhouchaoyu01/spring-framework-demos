package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.ReconciliationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconciliationTaskRepository extends JpaRepository<ReconciliationTaskEntity, Long> {
    List<ReconciliationTaskEntity> findByStatus(Integer status);
    List<ReconciliationTaskEntity> findByType(Integer type);
}