package com.firstproject.framevalue.repository;

import com.firstproject.framevalue.entity.UserSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserSubmissionRepository - Data access for community FPS submissions.
 * Enforces daily limits and provides queries for validation and statistics.
 */

@Repository
public interface UserSubmissionRepository extends JpaRepository<UserSubmission, Long> {
    List<UserSubmission> findByUserIpAndSubmittedAtAfter(String userIp, LocalDateTime after);
    List<UserSubmission> findByExpiresAtBefore(LocalDateTime now);
    List<UserSubmission> findBySubmittedAtBefore(LocalDateTime cutoffTime);

    long countByGpuIdAndGameNameAndMode(Long gpuId, String gameName, String mode);

    @Query("SELECT COUNT(us) FROM UserSubmission us WHERE us.userIp = :userIp AND us.submittedAt >= :startOfDay")
    long countByUserIpToday(@Param("userIp") String userIp, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(us) FROM UserSubmission us WHERE us.gpu.id = :gpuId AND us.gameName = :gameName AND us.mode = :mode AND us.submittedAt >= :startOfDay")
    long countTodayByGpuAndGameAndMode(@Param("gpuId") Long gpuId, @Param("gameName") String gameName, @Param("mode") String mode, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT us FROM UserSubmission us WHERE us.gpu.id = :gpuId AND us.gameName = :gameName AND us.mode = :mode")
    List<UserSubmission> findAllByGpuAndGameAndMode(@Param("gpuId") Long gpuId, @Param("gameName") String gameName, @Param("mode") String mode);

    @Query("SELECT COUNT(us) FROM UserSubmission us WHERE us.userIp = :userIp AND us.gpu.id = :gpuId AND us.gameName = :gameName AND us.submittedAt >= :startOfDay")
    long countByUserIpAndGameToday(@Param("userIp") String userIp, @Param("gpuId") Long gpuId, @Param("gameName") String gameName, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT s FROM UserSubmission s WHERE s.gpu.id = :gpuId " +
            "AND (:gameName IS NULL OR s.gameName = :gameName)")
    List<UserSubmission> findAllByGpuAndGame(@Param("gpuId") Long gpuId,
                                             @Param("gameName") String gameName);
}