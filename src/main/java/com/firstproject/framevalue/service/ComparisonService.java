package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ComparisonService - Business logic for side-by-side GPU comparison.
 * Finds common games, calculates FPS differences, and determines better value.
 */

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final GpuModelRepository gpuRepository;
    private final BenchmarkResultRepository benchmarkRepository;

    public List<GpuModel> getGpusForResolution(String resolution) {
        List<GpuModel> allGpus = gpuRepository.findAll();

        List<BenchmarkResult> allBenchmarks = benchmarkRepository.findAll();

        Map<Long, List<BenchmarkResult>> benchmarksByGpu = allBenchmarks.stream()
                .collect(Collectors.groupingBy(b -> b.getGpu().getId()));

        return allGpus.stream()
                .filter(gpu -> {
                    List<BenchmarkResult> gpuBenchmarks = benchmarksByGpu.getOrDefault(gpu.getId(), List.of());
                    return gpuBenchmarks.stream()
                            .anyMatch(b -> resolution.equals(b.getResolution()));
                })
                .collect(Collectors.toList());
    }

    public ComparisonResult compareGpus(Long gpu1Id, Long gpu2Id, String resolution, String mode) {
        GpuModel gpu1 = gpuRepository.findById(gpu1Id).orElse(null);
        GpuModel gpu2 = gpuRepository.findById(gpu2Id).orElse(null);

        if (gpu1 == null || gpu2 == null) {
            return null;
        }

        if (gpu1.getLaunchPrice().compareTo(BigDecimal.ZERO) == 0 ||
                gpu2.getLaunchPrice().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        List<BenchmarkResult> allBenchmarks = benchmarkRepository.findAll();

        List<BenchmarkResult> benchmarks1 = allBenchmarks.stream()
                .filter(b -> b.getGpu().getId().equals(gpu1Id) && resolution.equals(b.getResolution()))
                .collect(Collectors.toList());

        List<BenchmarkResult> benchmarks2 = allBenchmarks.stream()
                .filter(b -> b.getGpu().getId().equals(gpu2Id) && resolution.equals(b.getResolution()))
                .collect(Collectors.toList());

        List<GameComparison> gameComparisons = getCommonGames(benchmarks1, benchmarks2, mode);

        if (gameComparisons.isEmpty()) {
            return null;
        }

        double avgFps1 = gameComparisons.stream()
                .mapToInt(GameComparison::getFps1)
                .average()
                .orElse(0.0);

        double avgFps2 = gameComparisons.stream()
                .mapToInt(GameComparison::getFps2)
                .average()
                .orElse(0.0);

        double fpsPerShekel1 = avgFps1 / gpu1.getLaunchPrice().doubleValue();
        double fpsPerShekel2 = avgFps2 / gpu2.getLaunchPrice().doubleValue();

        String betterValue;
        double diff = Math.abs(fpsPerShekel1 - fpsPerShekel2);

        if (diff < 0.001) {
            betterValue = "Equal";
        } else {
            betterValue = fpsPerShekel1 > fpsPerShekel2 ? gpu1.getModelName() : gpu2.getModelName();
        }

        return new ComparisonResult(
                gpu1, gpu2, resolution, mode,
                gameComparisons, avgFps1, avgFps2,
                fpsPerShekel1, fpsPerShekel2, betterValue
        );
    }

    private List<GameComparison> getCommonGames(List<BenchmarkResult> benchmarks1,
                                                List<BenchmarkResult> benchmarks2,
                                                String mode) {
        List<GameComparison> comparisons = new ArrayList<>();

        Map<String, BenchmarkResult> gamesMap1 = benchmarks1.stream()
                .collect(Collectors.toMap(
                        BenchmarkResult::getGameName,
                        b -> b,
                        (a, b) -> a
                ));

        for (BenchmarkResult bench2 : benchmarks2) {
            String gameName = bench2.getGameName();
            BenchmarkResult bench1 = gamesMap1.get(gameName);

            if (bench1 != null) {
                Integer fps1 = "ultra".equalsIgnoreCase(mode) ? bench1.getFpsUltra() : bench1.getFpsLow();
                Integer fps2 = "ultra".equalsIgnoreCase(mode) ? bench2.getFpsUltra() : bench2.getFpsLow();

                if (fps1 != null && fps2 != null) {
                    int diff = fps2 - fps1;
                    double diffPercent = ((double) diff / fps1) * 100;

                    comparisons.add(new GameComparison(
                            gameName, fps1, fps2, diff, diffPercent
                    ));
                }
            }
        }

        return comparisons;
    }

    @Getter
    @AllArgsConstructor
    public static class ComparisonResult {
        private GpuModel gpu1;
        private GpuModel gpu2;
        private String resolution;
        private String mode;
        private List<GameComparison> gameComparisons;
        private double avgFps1;
        private double avgFps2;
        private double fpsPerShekel1;
        private double fpsPerShekel2;
        private String betterValue;
    }

    @Getter
    @AllArgsConstructor
    public static class GameComparison {
        private String gameName;
        private int fps1;
        private int fps2;
        private int diff;
        private double diffPercent;
    }
}