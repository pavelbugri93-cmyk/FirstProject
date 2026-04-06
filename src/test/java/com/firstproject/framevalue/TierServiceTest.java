package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.service.TierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TierService.
 * Tests tier grouping, FPS/₪ calculation, and AMD vs NVIDIA winner determination.
 */
@ExtendWith(MockitoExtension.class)
class TierServiceTest {

    @Mock
    private GpuModelRepository gpuRepository;

    @Mock
    private BenchmarkResultRepository benchmarkRepository;

    @InjectMocks
    private TierService tierService;

    private GpuModel amdGpu1;
    private GpuModel nvidiaGpu1;
    private BenchmarkResult amdBenchmark1;
    private BenchmarkResult nvidiaBenchmark1;

    @BeforeEach
    void setUp() {
        amdGpu1 = new GpuModel();
        amdGpu1.setId(1L);
        amdGpu1.setModelName("RX 7600");
        amdGpu1.setManufacturer("AMD");
        amdGpu1.setTier(1);
        amdGpu1.setLaunchPrice(BigDecimal.valueOf(2000));
        amdGpu1.setRecommendedResolution("1080p");

        amdBenchmark1 = new BenchmarkResult();
        amdBenchmark1.setGpu(amdGpu1);
        amdBenchmark1.setGameName("Cyberpunk 2077");
        amdBenchmark1.setFpsUltra(90);

        nvidiaGpu1 = new GpuModel();
        nvidiaGpu1.setId(3L);
        nvidiaGpu1.setModelName("RTX 4060");
        nvidiaGpu1.setManufacturer("NVIDIA");
        nvidiaGpu1.setTier(1);
        nvidiaGpu1.setLaunchPrice(BigDecimal.valueOf(2200));
        nvidiaGpu1.setRecommendedResolution("1080p");

        nvidiaBenchmark1 = new BenchmarkResult();
        nvidiaBenchmark1.setGpu(nvidiaGpu1);
        nvidiaBenchmark1.setGameName("Cyberpunk 2077");
        nvidiaBenchmark1.setFpsUltra(95);
    }

    // ========== Test 1: Calculate FPS/₪ within Tier ==========
    @Test
    void testGetTierData_CalculatesFpsPerShekel_Correctly() {
        when(gpuRepository.findAll()).thenReturn(List.of(amdGpu1));
        when(benchmarkRepository.findAll()).thenReturn(List.of(amdBenchmark1));

        TierService.TierData result = tierService.getTierData(1);

        TierService.GpuWithStats amdGpu = result.getAmdGpus().get(0);
        assertEquals(0.045, amdGpu.getFpsPerShekel(), 0.001);
        assertEquals(90.0, amdGpu.getAvgFps(), 0.01);
    }

    // ========== Test 2: Determine Winner - AMD vs NVIDIA ==========
    @Test
    void testDetermineWinner_AmdBetter_ReturnsAmd() {
        amdGpu1.setLaunchPrice(BigDecimal.valueOf(2000)); // 90/2000 = 0.045
        nvidiaGpu1.setLaunchPrice(BigDecimal.valueOf(3000)); // 95/3000 = 0.0317

        when(gpuRepository.findAll()).thenReturn(List.of(amdGpu1, nvidiaGpu1));
        when(benchmarkRepository.findAll()).thenReturn(List.of(amdBenchmark1, nvidiaBenchmark1));

        TierService.TierData tierData = tierService.getTierData(1);
        TierService.WinnerInfo winner = tierService.determineWinner(tierData);

        assertNotNull(winner);
        assertEquals("RX 7600", winner.getModelName());
        assertEquals("1080p", winner.getRecommendedResolution());
    }

    // ========== Test 3: Filter by Tier - Only Tier 1 ==========
    @Test
    void testGetTierData_Tier1_FiltersCorrectly() {
        GpuModel tier2Gpu = new GpuModel();
        tier2Gpu.setId(4L);
        tier2Gpu.setModelName("RTX 4070");
        tier2Gpu.setManufacturer("NVIDIA");
        tier2Gpu.setTier(2); // Tier 2!
        tier2Gpu.setLaunchPrice(BigDecimal.valueOf(3000));

        when(gpuRepository.findAll()).thenReturn(List.of(amdGpu1, nvidiaGpu1, tier2Gpu));
        when(benchmarkRepository.findAll()).thenReturn(List.of(amdBenchmark1, nvidiaBenchmark1));

        TierService.TierData result = tierService.getTierData(1);

        assertEquals(1, result.getAmdGpus().size()); // Only AMD Tier 1
        assertEquals(1, result.getNvidiaGpus().size()); // Only NVIDIA Tier 1
    }
}