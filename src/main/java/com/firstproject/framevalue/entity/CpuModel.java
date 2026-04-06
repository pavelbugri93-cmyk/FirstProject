package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CpuModel - Represents CPU used in benchmark tests.
 * All benchmarks were conducted with Ryzen 5 7500F.
 */

@Entity
@Table(name = "cpu_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CpuModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String manufacturer;

    @OneToMany(mappedBy = "cpu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BenchmarkResult> benchmarks = new ArrayList<>();
}