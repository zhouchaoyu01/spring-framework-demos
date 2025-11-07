package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    Optional<ScheduleEntity> findByTaskId(Long taskId);
}