package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CommunityAverage - Stores cumulative average FPS from user submissions (10+ reports required).
 * Updated automatically by DailyCleanupScheduler after processing community reports.
 */

@Entity
@Table(name = "community_averages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"gpu_id", "game_name", "resolution"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityAverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private GpuModel gpu;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(nullable = false, length = 20)
    private String resolution;

    @Column(name = "avg_community_fps", nullable = false)
    private Double avgCommunityFps;

    @Column(nullable = false)
    private Integer submissionCount;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}