package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.GpuModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * GpuModelRepository - Data access for GPU models with custom search queries.
 * Supports filtering by tier, manufacturer, and fuzzy name matching.
 */

@Repository
public interface GpuModelRepository extends JpaRepository<GpuModel, Long> {

    Optional<GpuModel> findByModelName(String modelName);

    List<GpuModel> findByTier(Integer tier);

    List<GpuModel> findByManufacturer(String manufacturer);


    @Query("SELECT g FROM GpuModel g WHERE g.modelName LIKE %:name%")
    List<GpuModel> findByModelNameContaining(@Param("name") String name);


    @Query("SELECT g FROM GpuModel g WHERE LOWER(g.modelName) = LOWER(:name)")
    Optional<GpuModel> findByModelNameIgnoreCase(@Param("name") String name);
}