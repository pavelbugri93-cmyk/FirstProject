package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.GpuPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * GpuPriceRepository - Data access for GPU pricing with batch query optimization.
 * Includes custom query to fetch latest prices for multiple GPUs in single operation.
 */

@Repository
public interface GpuPriceRepository extends JpaRepository<GpuPrice, Long> {

    List<GpuPrice> findByGpuId(Long gpuId);

    List<GpuPrice> findByGpuIdOrderByUpdatedAtDesc(Long gpuId);

    Optional<GpuPrice> findTopByGpuIdOrderByUpdatedAtDesc(Long gpuId);

    @Query("SELECT p FROM GpuPrice p WHERE p.gpu.id IN :gpuIds " +
            "AND p.updatedAt = (SELECT MAX(p2.updatedAt) FROM GpuPrice p2 WHERE p2.gpu.id = p.gpu.id)")
    List<GpuPrice> findLatestPricesForGpus(@Param("gpuIds") List<Long> gpuIds);
}