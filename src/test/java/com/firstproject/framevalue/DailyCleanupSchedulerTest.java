package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.CommunityAverage;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.UserSubmission;
import com.firstproject.framevalue.repository.CommunityAverageRepository;
import com.firstproject.framevalue.repository.UserSubmissionRepository;
import com.firstproject.framevalue.scheduler.DailyCleanupScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DailyCleanupScheduler.
 * Tests 10+ submission threshold, weighted average calculation, and cleanup.
 */
@ExtendWith(MockitoExtension.class)
class DailyCleanupSchedulerTest {

    @Mock
    private UserSubmissionRepository submissionRepository;

    @Mock
    private CommunityAverageRepository communityAverageRepository;

    @InjectMocks
    private DailyCleanupScheduler scheduler;

    private GpuModel testGpu;

    @BeforeEach
    void setUp() {
        testGpu = new GpuModel();
        testGpu.setId(1L);
        testGpu.setModelName("RTX 4060");
    }

    // ========== Test 1: Less than 10 submissions - not processed ==========
    @Test
    void testCleanup_LessThan10Submissions_NotProcessed() {
        List<UserSubmission> submissions = createSubmissions(5, 100);
        when(submissionRepository.findAll()).thenReturn(submissions);
        when(communityAverageRepository.findAll()).thenReturn(new ArrayList<>());

        scheduler.cleanupAndUpdateAverages();

        verify(communityAverageRepository, never()).saveAll(anyList());
        verify(submissionRepository).deleteAll(submissions);
    }

    // ========== Test 2: 10+ submissions - creates new average ==========
    @Test
    void testCleanup_10Submissions_CreatesNewAverage() {
        List<UserSubmission> submissions = createSubmissions(10, 100);
        when(submissionRepository.findAll()).thenReturn(submissions);
        when(communityAverageRepository.findAll()).thenReturn(new ArrayList<>());

        scheduler.cleanupAndUpdateAverages();

        verify(communityAverageRepository).saveAll(argThat(iterable -> {
            List<CommunityAverage> list = new ArrayList<>();
            iterable.forEach(list::add);

            if (list.size() != 1) return false;
            CommunityAverage avg = list.get(0);
            return avg.getAvgCommunityFps() == 100.0 && avg.getSubmissionCount() == 10;
        }));
        verify(submissionRepository).deleteAll(submissions);
    }

    // ========== Test 3: Update existing average - weighted calculation ==========
    @Test
    void testCleanup_UpdatesExistingAverage_WeightedCorrectly() {
        CommunityAverage existing = CommunityAverage.builder()
                .gpu(testGpu)
                .gameName("Cyberpunk 2077")
                .resolution("1080p")
                .avgCommunityFps(100.0)
                .submissionCount(20)
                .build();

        List<UserSubmission> newSubmissions = createSubmissions(10, 200);

        when(submissionRepository.findAll()).thenReturn(newSubmissions);
        when(communityAverageRepository.findAll()).thenReturn(List.of(existing));

        scheduler.cleanupAndUpdateAverages();

        verify(communityAverageRepository).saveAll(argThat(iterable -> {
            List<CommunityAverage> list = new ArrayList<>();
            iterable.forEach(list::add);

            if (list.size() != 1) return false;
            CommunityAverage updated = list.get(0);

            double expectedAvg = (100.0 * 20 + 200.0 * 10) / 30.0;
            return Math.abs(updated.getAvgCommunityFps() - expectedAvg) < 0.01 &&
                    updated.getSubmissionCount() == 30;
        }));
    }

    // ========== Helper Methods ==========

    private List<UserSubmission> createSubmissions(int count, int fps) {
        List<UserSubmission> submissions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserSubmission sub = UserSubmission.builder()
                    .gpu(testGpu)
                    .gameName("Cyberpunk 2077")
                    .resolution("1080p")
                    .mode("ultra")
                    .reportedFps(fps)
                    .userIp("192.168.1." + i)
                    .submittedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            submissions.add(sub);
        }
        return submissions;
    }
}