package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.CpuModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * CpuModelRepository - Data access for CPU models used in benchmarks.
 * All tests conducted with Ryzen 5 7500F.
 */

@Repository
public interface CpuModelRepository extends JpaRepository<CpuModel, Long> {
    Optional<CpuModel> findByModelName(String modelName);
}