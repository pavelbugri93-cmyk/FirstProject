package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.service.ComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComparisonService.
 * Tests FPS/₪ calculation, winner determination, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private GpuModelRepository gpuRepository;

    @Mock
    private BenchmarkResultRepository benchmarkRepository;

    @InjectMocks
    private ComparisonService comparisonService;

    private GpuModel gpu1;
    private GpuModel gpu2;
    private GpuModel gpu3;
    private BenchmarkResult benchmark1a;
    private BenchmarkResult benchmark2a;

    @BeforeEach
    void setUp() {
        gpu1 = new GpuModel();
        gpu1.setId(1L);
        gpu1.setModelName("RTX 4060");
        gpu1.setManufacturer("NVIDIA");
        gpu1.setLaunchPrice(BigDecimal.valueOf(2000));

        gpu2 = new GpuModel();
        gpu2.setId(2L);
        gpu2.setModelName("RTX 4070");
        gpu2.setManufacturer("NVIDIA");
        gpu2.setLaunchPrice(BigDecimal.valueOf(3000));

        gpu3 = new GpuModel();
        gpu3.setId(3L);
        gpu3.setModelName("RTX 4080");
        gpu3.setManufacturer("NVIDIA");
        gpu3.setLaunchPrice(BigDecimal.ZERO);

        benchmark1a = new BenchmarkResult();
        benchmark1a.setGpu(gpu1);
        benchmark1a.setGameName("Cyberpunk 2077");
        benchmark1a.setResolution("1080p");
        benchmark1a.setFpsUltra(100);
        benchmark1a.setFpsLow(150);

        benchmark2a = new BenchmarkResult();
        benchmark2a.setGpu(gpu2);
        benchmark2a.setGameName("Cyberpunk 2077");
        benchmark2a.setResolution("1080p");
        benchmark2a.setFpsUltra(150);
        benchmark2a.setFpsLow(200);
    }

    // ========== Test 1: Calculate FPS/₪ - Core project feature ==========
    @Test
    void testCompareGpus_CalculatesFpsPerShekel_Correctly() {
        when(gpuRepository.findById(1L)).thenReturn(Optional.of(gpu1));
        when(gpuRepository.findById(2L)).thenReturn(Optional.of(gpu2));
        when(benchmarkRepository.findAll()).thenReturn(List.of(benchmark1a, benchmark2a));

        ComparisonService.ComparisonResult result =
                comparisonService.compareGpus(1L, 2L, "1080p", "ultra");

        assertEquals(0.05, result.getFpsPerShekel1(), 0.001);
        assertEquals(0.05, result.getFpsPerShekel2(), 0.001);
    }

    // ========== Test 2: Determine Winner - GPU1 better value ==========
    @Test
    void testCompareGpus_Gpu1BetterValue_ReturnsGpu1AsWinner() {
        gpu1.setLaunchPrice(BigDecimal.valueOf(2000));
        gpu2.setLaunchPrice(BigDecimal.valueOf(4000));

        when(gpuRepository.findById(1L)).thenReturn(Optional.of(gpu1));
        when(gpuRepository.findById(2L)).thenReturn(Optional.of(gpu2));
        when(benchmarkRepository.findAll()).thenReturn(List.of(benchmark1a, benchmark2a));

        ComparisonService.ComparisonResult result =
                comparisonService.compareGpus(1L, 2L, "1080p", "ultra");

        assertEquals("RTX 4060", result.getBetterValue());
    }

    // ========== Test 3: Protection - Price 0 returns null ==========
    @Test
    void testCompareGpus_Gpu1PriceZero_ReturnsNull() {
        when(gpuRepository.findById(3L)).thenReturn(Optional.of(gpu3));
        when(gpuRepository.findById(2L)).thenReturn(Optional.of(gpu2));

        ComparisonService.ComparisonResult result =
                comparisonService.compareGpus(3L, 2L, "1080p", "ultra");

        assertNull(result);
    }
}