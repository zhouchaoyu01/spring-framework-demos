package com.coding.cz.recon.repository;

import com.coding.cz.recon.entity.RdcTaskDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */
@Repository
public interface RdcTaskDetailRepository extends JpaRepository<RdcTaskDetail, Long> {
    List<RdcTaskDetail> findByTaskId(Long taskId);
}