package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.CommunityAverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * CommunityAverageRepository - Data access for cumulative community FPS averages.
 * Stores processed averages from 10+ user submissions per game/resolution.
 */

@Repository
public interface CommunityAverageRepository extends JpaRepository<CommunityAverage, Long> {

    Optional<CommunityAverage> findByGpuIdAndGameNameAndResolution(Long gpuId, String gameName, String resolution);

}