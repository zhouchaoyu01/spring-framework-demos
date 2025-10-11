package com.coding.cz.recon.repository;

import com.coding.cz.recon.entity.RdcTaskConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */
@Repository
public interface RdcTaskConfigRepository extends JpaRepository<RdcTaskConfig, Long> {
    List<RdcTaskConfig> findByStatus(String status);
}
