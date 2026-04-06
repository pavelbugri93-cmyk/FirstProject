package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.UserSubmissionRepository;
import com.firstproject.framevalue.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SubmissionService.
 * Tests 40% deviation validation and 3/day rate limiting.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private GpuModelRepository gpuRepository;

    @Mock
    private BenchmarkResultRepository benchmarkRepository;

    @Mock
    private UserSubmissionRepository submissionRepository;

    @InjectMocks
    private SubmissionService submissionService;

    private GpuModel testGpu;
    private BenchmarkResult testBenchmark;

    @BeforeEach
    void setUp() {
        testGpu = new GpuModel();
        testGpu.setId(1L);
        testGpu.setModelName("RTX 4060");
        testGpu.setLaunchPrice(BigDecimal.valueOf(2000));

        testBenchmark = new BenchmarkResult();
        testBenchmark.setGpu(testGpu);
        testBenchmark.setGameName("Cyberpunk 2077");
        testBenchmark.setResolution("1080p");
        testBenchmark.setFpsUltra(100);
    }

    // ========== Test 1: Validation - Valid report (within 40%) ==========
    @Test
    void testValidateSubmission_ValidFps_ReturnsTrue() {
        when(benchmarkRepository.findByGpuId(1L)).thenReturn(List.of(testBenchmark));

        SubmissionService.ValidationResult result =
                submissionService.validateSubmission(1L, "Cyberpunk 2077", "ultra", 110);

        assertTrue(result.isValid());
        assertEquals(100, result.getSystemFps());
        assertTrue(result.getDeviation() < 40);
    }

    // ========== Test 2: Validation - Report over 40% rejected ==========
    @Test
    void testValidateSubmission_TooHighFps_ReturnsFalse() {
        when(benchmarkRepository.findByGpuId(1L)).thenReturn(List.of(testBenchmark));

        SubmissionService.ValidationResult result =
                submissionService.validateSubmission(1L, "Cyberpunk 2077", "ultra", 200);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("deviates"));
        assertTrue(result.getDeviation() > 40);
    }

    // ========== Test 3: Daily limit - 3 reports per day ==========
    @Test
    void testHasReachedDailyLimit_3Reports_ReturnsTrue() {
        when(submissionRepository.countByUserIpAndGameToday(
                eq("192.168.1.1"), eq(1L), eq("Cyberpunk 2077"), any()))
                .thenReturn(3L);

        boolean result = submissionService.hasReachedDailyLimitForGame(
                "192.168.1.1", 1L, "Cyberpunk 2077");

        assertTrue(result);
    }
}