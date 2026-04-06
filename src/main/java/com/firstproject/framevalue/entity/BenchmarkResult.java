package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * BenchmarkResult - Stores GPU benchmark data (FPS) for specific games and resolutions.
 * Contains both Low and Ultra graphics settings results.
 */

@Entity
@Table(name = "benchmark_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"gpu_id", "cpu_id", "game_name", "resolution"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchmarkResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private GpuModel gpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id", nullable = false)
    private CpuModel cpu;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(nullable = false, length = 20)
    private String resolution;

    @Column
    private Integer fpsLow;

    @Column
    private Integer fpsUltra;

    @Column(length = 500)
    private String settingsDetails;

    @Column(length = 500)
    private String sourceUrl;
}