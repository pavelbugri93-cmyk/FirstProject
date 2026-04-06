package com.firstproject.framevalue.scheduler;

import com.firstproject.framevalue.entity.CommunityAverage;
import com.firstproject.framevalue.entity.UserSubmission;
import com.firstproject.framevalue.repository.CommunityAverageRepository;
import com.firstproject.framevalue.repository.UserSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DailyCleanupScheduler - Processes user FPS submissions into cumulative averages.
 * Runs daily at midnight. Requires 10+ submissions to create/update averages, then deletes processed reports.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyCleanupScheduler {

    private final UserSubmissionRepository submissionRepository;
    private final CommunityAverageRepository communityAverageRepository;


    //@Scheduled(cron = "0 0 0 * * ?")// Every day at midnight (00:00)
    @Transactional
    public void cleanupAndUpdateAverages() {
        log.info("========== Starting averages update and cleanup ==========");

        try {
            List<UserSubmission> allSubmissions = submissionRepository.findAll();

            if (allSubmissions.isEmpty()) {
                log.info("No submissions to process");
                return;
            }

            log.info("Found {} submissions to process", allSubmissions.size());

            List<CommunityAverage> existingAverages = communityAverageRepository.findAll();

            Map<GroupKey, CommunityAverage> existingMap = existingAverages.stream()
                    .collect(Collectors.toMap(
                            avg -> new GroupKey(avg.getGpu().getId(), avg.getGameName(), avg.getResolution()),
                            avg -> avg
                    ));

            log.info("Found {} existing averages", existingMap.size());

            Map<GroupKey, List<UserSubmission>> groupedSubmissions = allSubmissions.stream()
                    .collect(Collectors.groupingBy(sub ->
                            new GroupKey(sub.getGpu().getId(), sub.getGameName(), sub.getResolution())
                    ));

            log.info("Found {} unique combinations (GPU + game + resolution)", groupedSubmissions.size());

            List<CommunityAverage> toSave = new ArrayList<>();
            int updatedCount = 0;
            int createdCount = 0;

            for (Map.Entry<GroupKey, List<UserSubmission>> entry : groupedSubmissions.entrySet()) {
                GroupKey key = entry.getKey();
                List<UserSubmission> submissions = entry.getValue();

                long validCount = submissions.stream()
                        .filter(sub -> sub.getReportedFps() != null && sub.getReportedFps() > 0)
                        .count();

                if (validCount < 10) {
                    log.warn("Not enough reports: {} | {} | GPU {} - only {} reports (10+ required)",
                            key.gameName, key.resolution, key.gpuId, validCount);
                    continue;
                }

                double sumNewFps = 0;
                int validSubmissions = 0;

                for (UserSubmission sub : submissions) {
                    Integer fps = sub.getReportedFps();
                    if (fps != null && fps > 0) {
                        sumNewFps += fps;
                        validSubmissions++;
                    }
                }

                int newSubmissionCount = validSubmissions;
                double avgNewFps = sumNewFps / newSubmissionCount;

                CommunityAverage existing = existingMap.get(key);

                if (existing != null) {
                    double oldWeightedSum = existing.getAvgCommunityFps() * existing.getSubmissionCount();
                    int totalSubmissions = existing.getSubmissionCount() + newSubmissionCount;
                    double newAverage = (oldWeightedSum + sumNewFps) / totalSubmissions;

                    log.debug("Updating: {} | {} | {} - previous avg: {:.1f} FPS ({} reports) → new: {:.1f} FPS ({} reports)",
                            existing.getGpu().getModelName(), key.gameName, key.resolution,
                            existing.getAvgCommunityFps(), existing.getSubmissionCount(),
                            newAverage, totalSubmissions);

                    existing.setAvgCommunityFps(newAverage);
                    existing.setSubmissionCount(totalSubmissions);
                    toSave.add(existing);
                    updatedCount++;

                } else {
                    CommunityAverage newAvg = CommunityAverage.builder()
                            .gpu(submissions.get(0).getGpu())
                            .gameName(key.gameName)
                            .resolution(key.resolution)
                            .avgCommunityFps(avgNewFps)
                            .submissionCount(newSubmissionCount)
                            .build();

                    log.debug("Creating new: {} | {} | {} - average: {:.1f} FPS ({} reports)",
                            newAvg.getGpu().getModelName(), key.gameName, key.resolution,
                            avgNewFps, newSubmissionCount);

                    toSave.add(newAvg);
                    createdCount++;
                }
            }

            if (!toSave.isEmpty()) {
                communityAverageRepository.saveAll(toSave);
                log.info("Averages saved: {} updated | {} created", updatedCount, createdCount);
            }

            submissionRepository.deleteAll(allSubmissions);
            log.info("Deleted {} submissions", allSubmissions.size());

            log.info("========== Process completed successfully! ==========");

        } catch (Exception e) {
            log.error("Error updating averages: {}", e.getMessage(), e);
            throw e;
        }
    }

    private record GroupKey(Long gpuId, String gameName, String resolution) {}
}