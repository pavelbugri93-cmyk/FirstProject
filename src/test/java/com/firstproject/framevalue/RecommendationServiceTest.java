package com.firstproject.framevalue;

import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecommendationService.
 * Tests GPU recommendations, FPS/₪ calculation, and budget filtering.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GpuModelRepository gpuModelRepository;

    @Mock
    private GpuPriceRepository gpuPriceRepository;

    @Mock
    private BenchmarkResultRepository benchmarkResultRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private GpuModel gpu1;
    private GpuModel gpu2;
    private GpuPrice price1;
    private GpuPrice price2;
    private BenchmarkResult benchmark1;
    private BenchmarkResult benchmark2;

    @BeforeEach
    void setUp() {
        gpu1 = new GpuModel();
        gpu1.setId(1L);
        gpu1.setModelName("RTX 4060");
        gpu1.setManufacturer("NVIDIA");

        price1 = GpuPrice.builder()
                .gpu(gpu1)
                .currentPrice(BigDecimal.valueOf(2000))
                .updatedAt(LocalDateTime.now())
                .build();

        benchmark1 = new BenchmarkResult();
        benchmark1.setGpu(gpu1);
        benchmark1.setGameName("Cyberpunk 2077");
        benchmark1.setResolution("1080p");
        benchmark1.setFpsUltra(100);

        gpu2 = new GpuModel();
        gpu2.setId(2L);
        gpu2.setModelName("RTX 4070");
        gpu2.setManufacturer("NVIDIA");

        price2 = GpuPrice.builder()
                .gpu(gpu2)
                .currentPrice(BigDecimal.valueOf(3000))
                .updatedAt(LocalDateTime.now())
                .build();

        benchmark2 = new BenchmarkResult();
        benchmark2.setGpu(gpu2);
        benchmark2.setGameName("Cyberpunk 2077");
        benchmark2.setResolution("1080p");
        benchmark2.setFpsUltra(150);
    }

    // ========== Test 1: Calculate FPS/₪ in recommendation system ==========
    @Test
    void testGetRecommendations_CalculatesFpsPerShekel_Correctly() {
        when(gpuModelRepository.findAll()).thenReturn(List.of(gpu1));
        when(gpuPriceRepository.findAll()).thenReturn(List.of(price1));
        when(benchmarkResultRepository.findAll()).thenReturn(List.of(benchmark1));

        RecommendationService.RecommendationResponse response =
                recommendationService.getRecommendations(
                        5000, "1080p", "ultra", List.of("Cyberpunk 2077"));

        RecommendationService.RecommendationResult result = response.getRecommendations().get(0);
        assertEquals(0.05, result.getPricePerformanceRatio(), 0.001); // 100/2000
    }

    // ========== Test 2: Filter by budget ==========
    @Test
    void testGetRecommendations_FiltersByBudget() {
        when(gpuModelRepository.findAll()).thenReturn(List.of(gpu1, gpu2));
        when(gpuPriceRepository.findAll()).thenReturn(List.of(price1, price2));
        when(benchmarkResultRepository.findAll()).thenReturn(List.of(benchmark1, benchmark2));

        RecommendationService.RecommendationResponse response =
                recommendationService.getRecommendations(
                        2500, "1080p", "ultra", List.of("Cyberpunk 2077"));

        assertEquals(1, response.getRecommendations().size());
        assertEquals("RTX 4060", response.getRecommendations().get(0).getGpuName());
    }

    // ========== Test 3: Sort by Match Score - Best value first ==========
    @Test
    void testGetRecommendations_SortsByMatchScore() {
        when(gpuModelRepository.findAll()).thenReturn(List.of(gpu1, gpu2));
        when(gpuPriceRepository.findAll()).thenReturn(List.of(price1, price2));
        when(benchmarkResultRepository.findAll()).thenReturn(List.of(benchmark1, benchmark2));

        RecommendationService.RecommendationResponse response =
                recommendationService.getRecommendations(
                        5000, "1080p", "ultra", List.of("Cyberpunk 2077"));

        assertEquals(2, response.getRecommendations().size());

        RecommendationService.RecommendationResult first = response.getRecommendations().get(0);
        RecommendationService.RecommendationResult second = response.getRecommendations().get(1);

        assertTrue(first.getMatchScore() >= second.getMatchScore());
    }
}