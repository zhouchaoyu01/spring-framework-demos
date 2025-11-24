package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */


import com.coding.cz.recon.entity.ExecutionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecordEntity, Long> {
    List<ExecutionRecordEntity> findByTaskIdOrderByStartTimeDesc(Long taskId);
    List<ExecutionRecordEntity> findByStatusAndStartTimeBetween(Integer status, LocalDateTime start, LocalDateTime end);
}
