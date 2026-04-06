package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * GpuModel - Represents a graphics card with benchmarks, pricing, and community data.
 * Categorized by performance tier (1-5) and recommended resolution.
 */

@Entity
@Table(name = "gpu_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpuModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private Integer tier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal launchPrice;

    @Column(length = 100)
    private String recommendedResolution;

    @OneToMany(mappedBy = "gpu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BenchmarkResult> benchmarks = new ArrayList<>();

    @OneToMany(mappedBy = "gpu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GpuPrice> prices = new ArrayList<>();

    @OneToMany(mappedBy = "gpu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserSubmission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "gpu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommunityAverage> communityAverages = new ArrayList<>();
}