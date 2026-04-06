package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.UserSubmission;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.UserSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * ResolutionService - Manages GPU benchmarks by resolution and calculates community-reported FPS averages.
 * Groups GPUs by resolution, retrieves benchmarks, and aggregates user submissions (requires 10+ reports to display average).
 */

@Service
@RequiredArgsConstructor
public class ResolutionService {

    private final GpuModelRepository gpuRepository;
    private final BenchmarkResultRepository benchmarkRepository;
    private final UserSubmissionRepository submissionRepository;

    public Map<String, List<GpuModel>> getGpusByResolution() {
        List<GpuModel> allGpus = gpuRepository.findAll();
        List<BenchmarkResult> allBenchmarks = benchmarkRepository.findAll();

        Map<Long, List<BenchmarkResult>> benchmarksByGpu = allBenchmarks.stream()
                .collect(Collectors.groupingBy(b -> b.getGpu().getId()));

        Map<String, List<GpuModel>> grouped = new LinkedHashMap<>();
        String[] resolutions = {"1080p", "1440p"};

        for (String resolution : resolutions) {
            List<GpuModel> gpusForResolution = allGpus.stream()
                    .filter(gpu -> {
                        List<BenchmarkResult> gpuBenchmarks = benchmarksByGpu.getOrDefault(gpu.getId(), List.of());
                        return gpuBenchmarks.stream()
                                .anyMatch(b -> resolution.equals(b.getResolution()));
                    })
                    .collect(Collectors.toList());

            if (!gpusForResolution.isEmpty()) {
                grouped.put(resolution, gpusForResolution);
            }
        }

        return grouped;
    }

    public List<BenchmarkResult> getBenchmarksForGpuAndResolution(Long gpuId, String resolution) {
        return benchmarkRepository.findByGpuId(gpuId)
                .stream()
                .filter(b -> resolution.equals(b.getResolution()))
                .collect(Collectors.toList());
    }

    public double calculateAverageFps(Long gpuId, String resolution) {
        List<BenchmarkResult> benchmarks = getBenchmarksForGpuAndResolution(gpuId, resolution);

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

    public String getSourceUrl(Long gpuId, String resolution) {
        List<BenchmarkResult> benchmarks = getBenchmarksForGpuAndResolution(gpuId, resolution);

        if (benchmarks.isEmpty()) {
            return "";
        }

        String url = benchmarks.get(0).getSourceUrl();
        return url != null ? url : "";
    }

    public CommunityFpsInfo getCommunityFpsForGame(Long gpuId, String gameName, String resolution) {
        List<UserSubmission> submissions = submissionRepository.findAllByGpuAndGame(gpuId, gameName)
                .stream()
                .filter(s -> resolution.equals(s.getResolution()))
                .collect(Collectors.toList());

        int totalSubmissions = submissions.size();

        if (totalSubmissions < 10) {
            return new CommunityFpsInfo(totalSubmissions, 10, null, false);
        } else {
            double average = submissions.stream()
                    .mapToInt(UserSubmission::getReportedFps)
                    .average()
                    .orElse(0);

            return new CommunityFpsInfo(totalSubmissions, 10, (int) Math.round(average), true);
        }
    }

    public CommunityFpsInfo getCommunityFpsForGameAndMode(Long gpuId, String gameName, String resolution, String mode) {
        List<UserSubmission> submissions = submissionRepository.findAllByGpuAndGame(gpuId, gameName)
                .stream()
                .filter(s -> resolution.equals(s.getResolution()))
                .filter(s -> mode.equals(s.getMode()))
                .collect(Collectors.toList());

        int totalSubmissions = submissions.size();

        if (totalSubmissions < 10) {
            return new CommunityFpsInfo(totalSubmissions, 10, null, false);
        } else {
            double average = submissions.stream()
                    .mapToInt(UserSubmission::getReportedFps)
                    .average()
                    .orElse(0);

            return new CommunityFpsInfo(totalSubmissions, 10, (int) Math.round(average), true);
        }
    }

    public Map<String, Map<String, CommunityFpsInfo>> getCommunityFpsForAllGames(
            Long gpuId,
            String resolution,
            List<BenchmarkResult> benchmarks) {

        List<UserSubmission> allSubmissions = submissionRepository.findAllByGpuAndGame(gpuId, null);

        List<UserSubmission> filteredSubmissions = allSubmissions.stream()
                .filter(s -> resolution.equals(s.getResolution()))
                .collect(Collectors.toList());

        Map<String, Map<String, CommunityFpsInfo>> result = new HashMap<>();

        for (BenchmarkResult benchmark : benchmarks) {
            Map<String, CommunityFpsInfo> modeMap = new HashMap<>();

            if (benchmark.getFpsUltra() != null) {
                List<UserSubmission> ultraSubs = filteredSubmissions.stream()
                        .filter(s -> benchmark.getGameName().equals(s.getGameName()))
                        .filter(s -> "ultra".equals(s.getMode()))
                        .collect(Collectors.toList());

                modeMap.put("ultra", calculateCommunityInfo(ultraSubs));
            }

            if (benchmark.getFpsLow() != null) {
                List<UserSubmission> lowSubs = filteredSubmissions.stream()
                        .filter(s -> benchmark.getGameName().equals(s.getGameName()))
                        .filter(s -> "low".equals(s.getMode()))
                        .collect(Collectors.toList());

                modeMap.put("low", calculateCommunityInfo(lowSubs));
            }

            result.put(benchmark.getGameName(), modeMap);
        }

        return result;
    }

    private CommunityFpsInfo calculateCommunityInfo(List<UserSubmission> submissions) {
        int totalSubmissions = submissions.size();

        if (totalSubmissions < 10) {
            return new CommunityFpsInfo(totalSubmissions, 10, null, false);
        } else {
            double average = submissions.stream()
                    .mapToInt(UserSubmission::getReportedFps)
                    .average()
                    .orElse(0);

            return new CommunityFpsInfo(totalSubmissions, 10, (int) Math.round(average), true);
        }
    }

    public Map<String, Integer> getTotalSubmissionsCountByMode(Long gpuId, String resolution) {
        List<UserSubmission> allSubmissions = submissionRepository.findAllByGpuAndGame(gpuId, null);

        List<UserSubmission> filteredByResolution = allSubmissions.stream()
                .filter(s -> resolution.equals(s.getResolution()))
                .collect(Collectors.toList());

        int ultraCount = (int) filteredByResolution.stream()
                .filter(s -> "ultra".equals(s.getMode()))
                .count();

        int lowCount = (int) filteredByResolution.stream()
                .filter(s -> "low".equals(s.getMode()))
                .count();

        Map<String, Integer> counts = new HashMap<>();
        counts.put("ultra", ultraCount);
        counts.put("low", lowCount);

        return counts;
    }

    @Getter
    @AllArgsConstructor
    public static class CommunityFpsInfo {
        private int currentSubmissions;
        private int requiredSubmissions;
        private Integer averageFps;
        private boolean hasEnough;
    }
}