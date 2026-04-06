package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.BenchmarkResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * BenchmarkResultRepository - Data access for GPU benchmark results.
 * Provides queries by GPU ID and game/resolution combinations.
 */

@Repository
public interface BenchmarkResultRepository extends JpaRepository<BenchmarkResult, Long> {
    List<BenchmarkResult> findByGpuId(Long gpuId);
    List<BenchmarkResult> findByGameNameAndResolution(String gameName, String resolution);
}