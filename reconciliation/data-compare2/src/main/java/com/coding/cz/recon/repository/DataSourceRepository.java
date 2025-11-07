package com.coding.cz.recon.repository;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */

import com.coding.cz.recon.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Long> {
    List<DataSourceEntity> findByStatus(Integer status);
}
