package com.firstproject.framevalue.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstproject.framevalue.entity.*;
import com.firstproject.framevalue.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataLoader - Imports GPU benchmark data from JSON file on application startup.
 * Runs once if database is empty. Loads GPUs, CPUs, benchmarks, and initial prices.
 */

@Configuration
@Slf4j
public class DataLoader {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Transactional
    public CommandLineRunner loadData(
            GpuModelRepository gpuRepository,
            CpuModelRepository cpuRepository,
            BenchmarkResultRepository benchmarkRepository,
            GpuPriceRepository priceRepository,
            ObjectMapper objectMapper) {

        return args -> {
            if (gpuRepository.count() > 0) {
                log.info("Database already contains data. Skipping import.");
                return;
            }

            log.info("Starting data import from benchmarks.json...");

            try {
                InputStream inputStream = new ClassPathResource("data/benchmarks.json").getInputStream();
                List<Map<String, Object>> gpuDataList = objectMapper.readValue(
                        inputStream,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );

                Map<String, CpuModel> cpuCache = new HashMap<>();

                for (Map<String, Object> gpuData : gpuDataList) {
                    try {
                        processGpuData(gpuData, gpuRepository, cpuRepository, benchmarkRepository, priceRepository, cpuCache);
                    } catch (Exception e) {
                        log.error("Error processing GPU data: {}", gpuData.get("gpu"), e);
                    }
                }

                log.info("Data import completed successfully! Imported {} GPUs with {} unique CPUs",
                        gpuDataList.size(), cpuCache.size());

            } catch (Exception e) {
                log.error("Failed to load benchmark data", e);
            }
        };
    }

    private void processGpuData(
            Map<String, Object> data,
            GpuModelRepository gpuRepository,
            CpuModelRepository cpuRepository,
            BenchmarkResultRepository benchmarkRepository,
            GpuPriceRepository priceRepository,
            Map<String, CpuModel> cpuCache) {


        String cpuName = ((String) data.get("cpu")).trim();


        CpuModel cpu = cpuCache.get(cpuName);

        if (cpu == null) {

            cpu = cpuRepository.findByModelName(cpuName)
                    .orElseGet(() -> {
                        CpuModel newCpu = CpuModel.builder()
                                .modelName(cpuName)
                                .manufacturer(cpuName.contains("Ryzen") ? "AMD" : "Intel")
                                .build();
                        return cpuRepository.save(newCpu);
                    });


            cpuCache.put(cpuName, cpu);
        }


        String gpuName = (String) data.get("gpu");
        GpuModel gpu = GpuModel.builder()
                .modelName(gpuName)
                .manufacturer(gpuName.contains("RTX") || gpuName.contains("GTX") ? "NVIDIA" : "AMD")
                .tier((Integer) data.get("tier"))
                .launchPrice(BigDecimal.valueOf(((Number) data.get("price_ils")).doubleValue()))
                .recommendedResolution((String) data.get("recommended_resolution"))
                .build();
        gpu = gpuRepository.save(gpu);


        GpuPrice price = GpuPrice.builder()
                .gpu(gpu)
                .currentPrice(gpu.getLaunchPrice())
                .previousPrice(null)
                .priceChangePercent(0.0)
                .sourceUrl((String) data.get("source_video"))
                .israelProductUrl((String) data.get("israel_product_url"))
                .build();
        priceRepository.save(price);


        @SuppressWarnings("unchecked")
        List<Map<String, Object>> benchmarks = (List<Map<String, Object>>) data.get("benchmarks");

        for (Map<String, Object> benchmark : benchmarks) {
            BenchmarkResult result = BenchmarkResult.builder()
                    .gpu(gpu)
                    .cpu(cpu)
                    .gameName((String) benchmark.get("game"))
                    .resolution((String) benchmark.get("res"))
                    .fpsLow(benchmark.get("low") != null ? ((Number) benchmark.get("low")).intValue() : null)
                    .fpsUltra(benchmark.get("ultra") != null ? ((Number) benchmark.get("ultra")).intValue() : null)
                    .settingsDetails((String) benchmark.get("options"))
                    .sourceUrl((String) data.get("source_video"))
                    .build();
            benchmarkRepository.save(result);
        }

        log.info(" Imported GPU: {} with {} benchmarks", gpuName, benchmarks.size());
    }
}