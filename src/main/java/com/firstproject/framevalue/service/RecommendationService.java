package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RecommendationService - Recommends GPUs based on budget, resolution, and selected games.
 * Uses optimized batch queries (only 3 DB calls) and calculates match scores based on FPS, price-performance ratio, and budget savings.
 */

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final GpuModelRepository gpuModelRepository;
    private final GpuPriceRepository gpuPriceRepository;
    private final BenchmarkResultRepository benchmarkResultRepository;

    public RecommendationResponse getRecommendations(
            Integer budget,
            String resolution,
            String mode,
            List<String> selectedGames) {

        List<GpuModel> allGpus = gpuModelRepository.findAll();
        int totalGpus = allGpus.size();

        List<GpuPrice> allPrices = gpuPriceRepository.findAll();


        Map<Long, GpuPrice> priceMap = allPrices.stream()
                .collect(Collectors.groupingBy(
                        price -> price.getGpu().getId(),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(GpuPrice::getUpdatedAt)),
                                opt -> opt.orElse(null)
                        )
                ));

        List<BenchmarkResult> allBenchmarks = benchmarkResultRepository.findAll();

        Map<Long, List<BenchmarkResult>> benchmarkMap = allBenchmarks.stream()
                .collect(Collectors.groupingBy(b -> b.getGpu().getId()));

        List<RecommendationResult> results = new ArrayList<>();
        int testedCount = 0;

        for (GpuModel gpu : allGpus) {
            GpuPrice price = priceMap.get(gpu.getId());
            if (price == null) continue;

            Integer priceValue = price.getCurrentPrice().intValue();
            if (budget != null && priceValue > budget) {
                continue;
            }

            RecommendationResult tempResult = new RecommendationResult(
                    gpu.getModelName(),
                    gpu.getManufacturer(),
                    priceValue,
                    0, 0, 0
            );

            double averageFps = calculateAverageFps(
                    gpu,
                    resolution,
                    mode,
                    selectedGames,
                    tempResult,
                    benchmarkMap
            );

            if (averageFps == 0) continue;

            testedCount++;

            double pricePerformanceRatio = priceValue > 0 ? averageFps / priceValue : 0.0;

            int matchScore = calculateMatchScore(averageFps, pricePerformanceRatio, priceValue, budget);

            RecommendationResult result = new RecommendationResult(
                    gpu.getModelName(),
                    gpu.getManufacturer(),
                    priceValue,
                    averageFps,
                    pricePerformanceRatio,
                    matchScore
            );

            result.setTotalGamesSelected(tempResult.getTotalGamesSelected());
            result.setGamesWithBenchmarks(tempResult.getGamesWithBenchmarks());
            for (String game : tempResult.getMissingGames()) {
                result.addMissingGame(game);
            }
            for (String game : tempResult.getAvailableGames()) {
                result.addAvailableGame(game);
            }

            results.add(result);
        }

        results.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        List<RecommendationResult> top3 = results.stream().limit(3).collect(Collectors.toList());

        return new RecommendationResponse(top3, testedCount, totalGpus);
    }

    private double calculateAverageFps(
            GpuModel gpu,
            String resolution,
            String mode,
            List<String> selectedGames,
            RecommendationResult result,
            Map<Long, List<BenchmarkResult>> benchmarkMap) {

        if (selectedGames == null || selectedGames.isEmpty()) {
            return 0;
        }

        result.setTotalGamesSelected(selectedGames.size());

        List<Double> fpsList = new ArrayList<>();

        List<BenchmarkResult> gpuBenchmarks = benchmarkMap.getOrDefault(gpu.getId(), Collections.emptyList());

        for (String gameName : selectedGames) {
            Optional<BenchmarkResult> benchmarkOpt = gpuBenchmarks.stream()
                    .filter(b -> b.getGameName().equals(gameName) && b.getResolution().equals(resolution))
                    .findFirst();

            if (!benchmarkOpt.isPresent()) {
                result.addMissingGame(gameName);
                continue;
            }

            BenchmarkResult benchmark = benchmarkOpt.get();
            Double fps = getFpsByMode(benchmark, mode);

            if (fps != null && fps > 0) {
                fpsList.add(fps);
                result.addAvailableGame(gameName);
            } else {
                result.addMissingGame(gameName);
            }
        }

        result.setGamesWithBenchmarks(fpsList.size());

        if (fpsList.isEmpty()) {
            return 0;
        }

        return fpsList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private Double getFpsByMode(BenchmarkResult benchmark, String mode) {
        Integer ultra = benchmark.getFpsUltra();
        Integer low = benchmark.getFpsLow();

        if ("ultra".equals(mode)) {
            return ultra != null ? ultra.doubleValue() : null;
        } else if ("low".equals(mode)) {
            return low != null ? low.doubleValue() : null;
        } else {
            if (ultra != null && low != null) {
                return (ultra + low) / 2.0;
            } else if (ultra != null) {
                return ultra.doubleValue();
            } else if (low != null) {
                return low.doubleValue();
            }
            return null;
        }
    }

    private int calculateMatchScore(double averageFps, double pricePerformanceRatio,
                                    Integer price, Integer budget) {
        int fpsScore = (int) Math.min(30, averageFps / 5);
        int ratioScore = (int) Math.min(50, pricePerformanceRatio * 666);

        int budgetScore = 0;
        if (budget != null && price <= budget) {
            double budgetUsage = (double) price / budget;
            double savingsRatio = 1 - budgetUsage;
            double effectiveSavings = Math.min(savingsRatio, 0.5);
            budgetScore = (int) (20 * (effectiveSavings / 0.5));
        }

        return Math.min(100, fpsScore + ratioScore + budgetScore);
    }

    @Getter
    @AllArgsConstructor
    public static class RecommendationResponse {
        private List<RecommendationResult> recommendations;
        private int totalTested;
        private int totalGpus;
    }

    public static class RecommendationResult {
        private String gpuName;
        private String manufacturer;
        private Integer price;
        private double averageFps;
        private double pricePerformanceRatio;
        private int matchScore;
        private int totalGamesSelected;
        private int gamesWithBenchmarks;
        private List<String> missingGames;
        private List<String> availableGames;

        public RecommendationResult(String gpuName, String manufacturer, Integer price,
                                    double averageFps, double pricePerformanceRatio,
                                    int matchScore) {
            this.gpuName = gpuName;
            this.manufacturer = manufacturer;
            this.price = price;
            this.averageFps = averageFps;
            this.pricePerformanceRatio = pricePerformanceRatio;
            this.matchScore = matchScore;
            this.missingGames = new ArrayList<>();
            this.availableGames = new ArrayList<>();
        }

        public String getGpuName() { return gpuName; }
        public String getManufacturer() { return manufacturer; }
        public Integer getPrice() { return price; }
        public double getAverageFps() { return averageFps; }
        public double getPricePerformanceRatio() { return pricePerformanceRatio; }
        public int getMatchScore() { return matchScore; }
        public int getTotalGamesSelected() { return totalGamesSelected; }
        public int getGamesWithBenchmarks() { return gamesWithBenchmarks; }
        public List<String> getMissingGames() { return missingGames; }
        public List<String> getAvailableGames() { return availableGames; }

        public void setTotalGamesSelected(int totalGamesSelected) {
            this.totalGamesSelected = totalGamesSelected;
        }
        public void setGamesWithBenchmarks(int gamesWithBenchmarks) {
            this.gamesWithBenchmarks = gamesWithBenchmarks;
        }
        public void addMissingGame(String gameName) {
            this.missingGames.add(gameName);
        }
        public void addAvailableGame(String gameName) {
            this.availableGames.add(gameName);
        }
        public boolean hasFullCoverage() {
            return gamesWithBenchmarks == totalGamesSelected;
        }
    }
}