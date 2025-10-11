package com.coding.cz.recon.repository;

import com.coding.cz.recon.entity.RdcReconciliationDiff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-11
 */
@Repository
public interface RdcReconciliationDiffRepository extends JpaRepository<RdcReconciliationDiff, Long> {
}
