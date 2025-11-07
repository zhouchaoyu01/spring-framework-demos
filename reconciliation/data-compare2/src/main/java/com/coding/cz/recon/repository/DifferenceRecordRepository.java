package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.DifferenceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DifferenceRecordRepository extends JpaRepository<DifferenceRecordEntity, Long> {
    List<DifferenceRecordEntity> findByTaskIdAndStatus(Long taskId, Integer status);
    List<DifferenceRecordEntity> findByDiscoveryTimeBetween(LocalDateTime start, LocalDateTime end);
}