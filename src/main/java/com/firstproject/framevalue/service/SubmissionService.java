package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.CommunityAverage;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.UserSubmission;
import com.firstproject.framevalue.repository.CommunityAverageRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.UserSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SubmissionService - Handles user-submitted FPS reports with validation and aggregation.
 * Validates submissions against system benchmarks (max 40% deviation), enforces daily limits (3 per game), and calculates both live and cumulative community averages (requires 10+ reports).
 */

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final GpuModelRepository gpuRepository;
    private final BenchmarkResultRepository benchmarkRepository;
    private final UserSubmissionRepository submissionRepository;
    private final CommunityAverageRepository communityAverageRepository;

    public List<GpuModel> getAllGpus() {
        return gpuRepository.findAll();
    }

    public List<String> getGamesForGpu(Long gpuId) {
        return benchmarkRepository.findByGpuId(gpuId)
                .stream()
                .map(BenchmarkResult::getGameName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public GameModeInfo getGameModeInfo(Long gpuId, String gameName, String userIp) {
        Optional<BenchmarkResult> benchmarkOpt = benchmarkRepository.findByGpuId(gpuId)
                .stream()
                .filter(b -> gameName.equals(b.getGameName()))
                .findFirst();

        if (benchmarkOpt.isEmpty()) {
            return null;
        }

        BenchmarkResult benchmark = benchmarkOpt.get();
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        List<UserSubmission> allSubmissions = submissionRepository.findAllByGpuAndGame(gpuId, gameName);

        List<UserSubmission> ultraSubmissions = allSubmissions.stream()
                .filter(s -> "ultra".equals(s.getMode()))
                .collect(Collectors.toList());

        List<UserSubmission> lowSubmissions = allSubmissions.stream()
                .filter(s -> "low".equals(s.getMode()))
                .collect(Collectors.toList());

        long ultraCount = ultraSubmissions.size();
        long lowCount = lowSubmissions.size();

        long ultraCountToday = ultraSubmissions.stream()
                .filter(s -> s.getSubmittedAt().isAfter(startOfDay))
                .count();

        long lowCountToday = lowSubmissions.stream()
                .filter(s -> s.getSubmittedAt().isAfter(startOfDay))
                .count();

        long userSubmissionsToday = allSubmissions.stream()
                .filter(s -> userIp.equals(s.getUserIp()))
                .filter(s -> s.getSubmittedAt().isAfter(startOfDay))
                .count();

        boolean hasUltra = benchmark.getFpsUltra() != null;
        boolean hasLow = benchmark.getFpsLow() != null;

        Integer ultraFps = hasUltra ?
                calculateAverageFps(ultraSubmissions, benchmark.getFpsUltra()) : null;
        Integer lowFps = hasLow ?
                calculateAverageFps(lowSubmissions, benchmark.getFpsLow()) : null;

        Integer cumulativeFps = null;
        Integer cumulativeCount = null;

        String resolution = benchmark.getResolution() != null ? benchmark.getResolution() : "1080p";
        Optional<CommunityAverage> savedAvg = communityAverageRepository
                .findByGpuIdAndGameNameAndResolution(gpuId, gameName, resolution);

        if (savedAvg.isPresent()) {
            cumulativeFps = savedAvg.get().getAvgCommunityFps().intValue();
            cumulativeCount = savedAvg.get().getSubmissionCount();
        }

        return new GameModeInfo(
                hasUltra, hasLow,
                ultraFps, lowFps,
                cumulativeFps, cumulativeCount,
                ultraCount, lowCount,
                ultraCountToday, lowCountToday,
                userSubmissionsToday
        );
    }

    private Integer calculateAverageFps(List<UserSubmission> submissions, Integer systemFps) {
        if (submissions.size() < 10) {
            return systemFps;
        }

        double average = submissions.stream()
                .mapToInt(UserSubmission::getReportedFps)
                .average()
                .orElse(systemFps);

        return (int) Math.round(average);
    }

    public boolean hasReachedDailyLimitForGame(String userIp, Long gpuId, String gameName) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long count = submissionRepository.countByUserIpAndGameToday(userIp, gpuId, gameName, startOfDay);
        return count >= 3;
    }

    public ValidationResult validateSubmission(Long gpuId, String gameName, String mode, int userFps) {
        List<BenchmarkResult> benchmarks = benchmarkRepository.findByGpuId(gpuId)
                .stream()
                .filter(b -> gameName.equals(b.getGameName()))
                .collect(Collectors.toList());

        if (benchmarks.isEmpty()) {
            return new ValidationResult(false, "Game not found for this GPU", 0, 0);
        }

        BenchmarkResult benchmark = benchmarks.get(0);
        Integer systemFps = "ultra".equalsIgnoreCase(mode) ? benchmark.getFpsUltra() : benchmark.getFpsLow();

        if (systemFps == null) {
            return new ValidationResult(false, "No FPS data in system for this mode", 0, 0);
        }

        double deviation = Math.abs(((double) userFps - systemFps) / systemFps * 100);

        if (deviation > 40) {
            return new ValidationResult(
                    false,
                    String.format("Your report (%d FPS) deviates %.0f%% from system average (%d FPS). Report rejected - deviation too high.",
                            userFps, deviation, systemFps),
                    systemFps,
                    deviation
            );
        }

        return new ValidationResult(true, "Report is valid and submitted successfully", systemFps, deviation);
    }

    public void saveSubmission(Long gpuId, String gameName, String mode, String resolution, int fps, String userIp) {
        GpuModel gpu = gpuRepository.findById(gpuId).orElse(null);
        if (gpu == null) {
            throw new IllegalArgumentException("GPU not found");
        }

        UserSubmission submission = UserSubmission.builder()
                .gpu(gpu)
                .gameName(gameName)
                .mode(mode)
                .resolution(resolution)
                .reportedFps(fps)
                .userIp(userIp)
                .build();

        submissionRepository.save(submission);
    }

    public String getResolutionForGame(Long gpuId, String gameName) {
        List<BenchmarkResult> benchmarks = benchmarkRepository.findByGpuId(gpuId)
                .stream()
                .filter(b -> gameName.equals(b.getGameName()))
                .collect(Collectors.toList());

        if (!benchmarks.isEmpty() && benchmarks.get(0).getResolution() != null) {
            return benchmarks.get(0).getResolution();
        }

        return "1920x1080";
    }

    public List<UserSubmissionDTO> getSubmissionsForGameAndMode(Long gpuId, String gameName, String mode) {
        List<UserSubmission> submissions = submissionRepository.findAllByGpuAndGameAndMode(gpuId, gameName, mode);

        return submissions.stream()
                .map(sub -> new UserSubmissionDTO(
                        sub.getReportedFps(),
                        sub.getSubmittedAt(),
                        sub.getUserIp().substring(0, Math.min(sub.getUserIp().length(), 10)) + "..."
                ))
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .collect(Collectors.toList());
    }

    @Getter
    @AllArgsConstructor
    public static class UserSubmissionDTO {
        private int fps;
        private LocalDateTime submittedAt;
        private String userIpPartial;
    }

    @Getter
    @AllArgsConstructor
    public static class GameModeInfo {
        private boolean hasUltra;
        private boolean hasLow;

        private Integer ultraFps;
        private Integer lowFps;

        private Integer cumulativeFps;
        private Integer cumulativeCount;

        private long ultraSubmissions;
        private long lowSubmissions;
        private long ultraSubmissionsToday;
        private long lowSubmissionsToday;
        private long userSubmissionsToday;
    }

    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private int systemFps;
        private double deviation;
    }
}