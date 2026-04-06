package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GpuService - Core business logic for GPU filtering, sorting, and FPS/₪ calculation.
 * Optimized with batch queries to avoid N+1 problems.
 */

@Service
@RequiredArgsConstructor
public class GpuService {

    private final GpuModelRepository gpuRepository;
    private final BenchmarkResultRepository benchmarkRepository;

    public List<GpuModel> getAllGpus() {
        return gpuRepository.findAll();
    }

    public List<GpuModel> getFilteredAndSortedGpus(
            String manufacturer,
            Integer budget,
            String sortBy) {

        List<GpuModel> gpus = getAllGpus();

        if (manufacturer != null && !manufacturer.isEmpty()) {
            gpus = filterByManufacturer(gpus, manufacturer);
        }

        if (budget != null) {
            gpus = filterByBudget(gpus, BigDecimal.valueOf(budget));
        }

        if ("fps_per_shekel".equals(sortBy)) {
            gpus = sortByFpsPerShekel(gpus);
            Collections.reverse(gpus);
        } else if ("price_asc".equals(sortBy)) {
            gpus = sortByPrice(gpus, true);
        } else {
            gpus = sortByFpsPerShekel(gpus);
            Collections.reverse(gpus);
        }

        return gpus;
    }

    public Map<Long, Double> calculateFpsPerShekelForAll(List<GpuModel> gpus) {
        List<BenchmarkResult> allBenchmarks = benchmarkRepository.findAll();

        Map<Long, List<BenchmarkResult>> benchmarksByGpu = allBenchmarks.stream()
                .collect(Collectors.groupingBy(b -> b.getGpu().getId()));

        Map<Long, Double> fpsMap = new HashMap<>();

        for (GpuModel gpu : gpus) {
            List<BenchmarkResult> benchmarks = benchmarksByGpu.getOrDefault(gpu.getId(), List.of());
            double avgFps = calculateAverageFpsFromList(benchmarks);

            double fpsPerShekel = gpu.getLaunchPrice().compareTo(BigDecimal.ZERO) > 0 ?
                    avgFps / gpu.getLaunchPrice().doubleValue() : 0.0;

            fpsMap.put(gpu.getId(), fpsPerShekel);
        }

        return fpsMap;
    }

    public List<GpuModel> filterByBudget(List<GpuModel> gpus, BigDecimal maxBudget) {
        if (maxBudget == null) return gpus;
        return gpus.stream()
                .filter(gpu -> gpu.getLaunchPrice().compareTo(maxBudget) <= 0)
                .collect(Collectors.toList());
    }

    public List<GpuModel> filterByManufacturer(List<GpuModel> gpus, String manufacturer) {
        if (manufacturer == null || manufacturer.isEmpty()) return gpus;
        return gpus.stream()
                .filter(gpu -> manufacturer.equalsIgnoreCase(gpu.getManufacturer()))
                .collect(Collectors.toList());
    }

    public List<GpuModel> filterByResolution(List<GpuModel> gpus, String resolution) {
        if (resolution == null || resolution.isEmpty()) return gpus;
        return gpus.stream()
                .filter(gpu -> resolution.equals(gpu.getRecommendedResolution()))
                .collect(Collectors.toList());
    }

    public List<GpuModel> sortByFpsPerShekel(List<GpuModel> gpus) {
        Map<Long, Double> fpsPerShekelMap = calculateFpsPerShekelForAll(gpus);

        return gpus.stream()
                .sorted((a, b) -> {
                    double fpsA = fpsPerShekelMap.getOrDefault(a.getId(), 0.0);
                    double fpsB = fpsPerShekelMap.getOrDefault(b.getId(), 0.0);
                    return Double.compare(fpsA, fpsB);
                })
                .collect(Collectors.toList());
    }

    public List<GpuModel> sortByPrice(List<GpuModel> gpus, boolean ascending) {
        return gpus.stream()
                .sorted((a, b) -> {
                    int compare = a.getLaunchPrice().compareTo(b.getLaunchPrice());
                    return ascending ? compare : -compare;
                })
                .collect(Collectors.toList());
    }

    private double calculateAverageFpsFromList(List<BenchmarkResult> benchmarks) {
        if (benchmarks.isEmpty()) {
            return 0.0;
        }

        double avgFps = benchmarks.stream()
                .filter(b -> b.getFpsUltra() != null)
                .mapToInt(BenchmarkResult::getFpsUltra)
                .average()
                .orElse(0.0);

        if (avgFps == 0.0) {
            avgFps = benchmarks.stream()
                    .filter(b -> b.getFpsLow() != null)
                    .mapToInt(BenchmarkResult::getFpsLow)
                    .average()
                    .orElse(0.0);
        }

        return avgFps;
    }

    @Deprecated
    public double calculateFpsPerShekel(GpuModel gpu) {
        List<BenchmarkResult> benchmarks = benchmarkRepository.findByGpuId(gpu.getId());
        double avgFps = calculateAverageFpsFromList(benchmarks);

        if (gpu.getLaunchPrice().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return avgFps / gpu.getLaunchPrice().doubleValue();
    }
}