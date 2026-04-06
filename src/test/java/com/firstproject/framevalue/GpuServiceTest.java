package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.service.GpuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GpuService.
 * Tests FPS/₪ calculation, budget filtering, and sorting.
 */
@ExtendWith(MockitoExtension.class)
class GpuServiceTest {

    @Mock
    private GpuModelRepository gpuRepository;

    @Mock
    private BenchmarkResultRepository benchmarkRepository;

    @InjectMocks
    private GpuService gpuService;

    private GpuModel amdGpu;
    private GpuModel nvidiaGpu1;
    private GpuModel nvidiaGpu2;
    private BenchmarkResult amdBenchmark;
    private BenchmarkResult nvidiaBenchmark1;
    private BenchmarkResult nvidiaBenchmark2;

    @BeforeEach
    void setUp() {
        amdGpu = new GpuModel();
        amdGpu.setId(1L);
        amdGpu.setModelName("RX 7600");
        amdGpu.setManufacturer("AMD");
        amdGpu.setLaunchPrice(BigDecimal.valueOf(2000));

        nvidiaGpu1 = new GpuModel();
        nvidiaGpu1.setId(2L);
        nvidiaGpu1.setModelName("RTX 4060");
        nvidiaGpu1.setManufacturer("NVIDIA");
        nvidiaGpu1.setLaunchPrice(BigDecimal.valueOf(2500));

        nvidiaGpu2 = new GpuModel();
        nvidiaGpu2.setId(3L);
        nvidiaGpu2.setModelName("RTX 4070");
        nvidiaGpu2.setManufacturer("NVIDIA");
        nvidiaGpu2.setLaunchPrice(BigDecimal.valueOf(3500));

        amdBenchmark = new BenchmarkResult();
        amdBenchmark.setGpu(amdGpu);
        amdBenchmark.setGameName("Cyberpunk 2077");
        amdBenchmark.setFpsUltra(90);

        nvidiaBenchmark1 = new BenchmarkResult();
        nvidiaBenchmark1.setGpu(nvidiaGpu1);
        nvidiaBenchmark1.setGameName("Cyberpunk 2077");
        nvidiaBenchmark1.setFpsUltra(100);

        nvidiaBenchmark2 = new BenchmarkResult();
        nvidiaBenchmark2.setGpu(nvidiaGpu2);
        nvidiaBenchmark2.setGameName("Cyberpunk 2077");
        nvidiaBenchmark2.setFpsUltra(150);
    }

    // ========== Test 1: Calculate FPS/₪ - Core project feature ==========
    @Test
    void testCalculateFpsPerShekelForAll_CalculatesCorrectly() {
        when(benchmarkRepository.findAll()).thenReturn(List.of(amdBenchmark, nvidiaBenchmark1));
        List<GpuModel> gpus = List.of(amdGpu, nvidiaGpu1);

        Map<Long, Double> result = gpuService.calculateFpsPerShekelForAll(gpus);

        assertEquals(2, result.size());
        assertEquals(0.045, result.get(1L), 0.001); // AMD: 90/2000
        assertEquals(0.04, result.get(2L), 0.001);  // NVIDIA: 100/2500
    }

    // ========== Test 2: Filter by budget ==========
    @Test
    void testFilterByBudget_3000_FiltersCorrectly() {
        List<GpuModel> allGpus = List.of(amdGpu, nvidiaGpu1, nvidiaGpu2);

        List<GpuModel> result = gpuService.filterByBudget(allGpus, BigDecimal.valueOf(3000));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(
                gpu -> gpu.getLaunchPrice().compareTo(BigDecimal.valueOf(3000)) <= 0));
    }

    // ========== Test 3: Sort by FPS/₪ - Find best value ==========
    @Test
    void testSortByFpsPerShekel_SortsCorrectly() {
        when(benchmarkRepository.findAll()).thenReturn(
                List.of(amdBenchmark, nvidiaBenchmark1, nvidiaBenchmark2));
        List<GpuModel> gpus = List.of(amdGpu, nvidiaGpu1, nvidiaGpu2);

        List<GpuModel> result = gpuService.sortByFpsPerShekel(gpus);

        assertEquals(3, result.size());
        assertEquals("RTX 4060", result.get(0).getModelName());  // 0.04 (lowest)
        assertEquals("RTX 4070", result.get(1).getModelName()); // 0.0428
        assertEquals("RX 7600", result.get(2).getModelName()); // 0.045 (highest)
    }
}