package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * TierService - Organizes GPUs by price tier and determines best value per manufacturer.
 * Fetches all benchmarks once, calculates FPS-per-shekel ratio, and ranks AMD vs NVIDIA GPUs within each tier to identify the winner.
 */

@Service
@RequiredArgsConstructor
public class TierService {

    private final GpuModelRepository gpuRepository;
    private final BenchmarkResultRepository benchmarkRepository;


    public TierData getTierData(Integer tier) {
        List<BenchmarkResult> allBenchmarks = benchmarkRepository.findAll();

        Map<Long, List<BenchmarkResult>> benchmarksByGpu = allBenchmarks.stream()
                .collect(Collectors.groupingBy(b -> b.getGpu().getId()));

        List<GpuModel> allGpus = gpuRepository.findAll().stream()
                .filter(gpu -> tier.equals(gpu.getTier()))
                .collect(Collectors.toList());

        if (allGpus.isEmpty()) {
            return new TierData(tier, new ArrayList<>(), new ArrayList<>());
        }

        List<GpuWithStats> amdGpus = allGpus.stream()
                .filter(gpu -> "AMD".equalsIgnoreCase(gpu.getManufacturer()))
                .map(gpu -> createGpuWithStats(gpu, benchmarksByGpu.getOrDefault(gpu.getId(), List.of())))
                .sorted(Comparator.comparingDouble(GpuWithStats::getFpsPerShekel).reversed())
                .collect(Collectors.toList());

        List<GpuWithStats> nvidiaGpus = allGpus.stream()
                .filter(gpu -> "NVIDIA".equalsIgnoreCase(gpu.getManufacturer()))
                .map(gpu -> createGpuWithStats(gpu, benchmarksByGpu.getOrDefault(gpu.getId(), List.of())))
                .sorted(Comparator.comparingDouble(GpuWithStats::getFpsPerShekel).reversed())
                .collect(Collectors.toList());

        return new TierData(tier, amdGpus, nvidiaGpus);
    }

    public WinnerInfo determineWinner(TierData tierData) {
        if (tierData.getAmdGpus().isEmpty() && tierData.getNvidiaGpus().isEmpty()) {
            return null;
        }

        if (tierData.getAmdGpus().isEmpty()) {
            GpuWithStats winner = tierData.getNvidiaGpus().get(0);
            return new WinnerInfo(winner.getModelName(), winner.getRecommendedResolution());
        }
        if (tierData.getNvidiaGpus().isEmpty()) {
            GpuWithStats winner = tierData.getAmdGpus().get(0);
            return new WinnerInfo(winner.getModelName(), winner.getRecommendedResolution());
        }

        GpuWithStats bestAmd = tierData.getAmdGpus().get(0);
        GpuWithStats bestNvidia = tierData.getNvidiaGpus().get(0);

        double diff = Math.abs(bestAmd.getFpsPerShekel() - bestNvidia.getFpsPerShekel());

        if (diff < 0.001) {
            return new WinnerInfo("Equal", null);
        }

        GpuWithStats winner = bestAmd.getFpsPerShekel() > bestNvidia.getFpsPerShekel() ?
                bestAmd : bestNvidia;

        return new WinnerInfo(winner.getModelName(), winner.getRecommendedResolution());
    }



    private GpuWithStats createGpuWithStats(GpuModel gpu, List<BenchmarkResult> benchmarks) {
        double avgFps = calculateAverageFps(benchmarks);
        double fpsPerShekel = gpu.getLaunchPrice().doubleValue() > 0 ?
                avgFps / gpu.getLaunchPrice().doubleValue() : 0.0;

        return new GpuWithStats(
                gpu.getId(),
                gpu.getModelName(),
                gpu.getLaunchPrice().doubleValue(),
                avgFps,
                fpsPerShekel,
                gpu.getRecommendedResolution()
        );
    }

    private double calculateAverageFps(List<BenchmarkResult> benchmarks) {
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


    @Getter
    @AllArgsConstructor
    public static class TierData {
        private Integer tier;
        private List<GpuWithStats> amdGpus;
        private List<GpuWithStats> nvidiaGpus;
    }


    @Getter
    @AllArgsConstructor
    public static class GpuWithStats {
        private Long id;
        private String modelName;
        private double price;
        private double avgFps;
        private double fpsPerShekel;
        private String recommendedResolution;
    }

    @Getter
    @AllArgsConstructor
    public static class WinnerInfo {
        private String modelName;
        private String recommendedResolution;
    }
}