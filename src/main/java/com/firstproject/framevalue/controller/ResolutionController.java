package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.BenchmarkResult;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.service.ResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ResolutionController - Groups GPUs by recommended resolution (1080p, 1440p).
 * Displays benchmark FPS, community reports, and submission counts for each GPU.
 */

@Controller
@RequiredArgsConstructor
public class ResolutionController {

    private final ResolutionService resolutionService;
    private final GpuPriceRepository priceRepository;

    @GetMapping("/resolution")
    public String resolutionPage(Model model) {
        Map<String, List<GpuModel>> gpusByResolution = resolutionService.getGpusByResolution();

        Map<String, Map<Long, Double>> fpsMap = new HashMap<>();
        Map<String, Map<Long, String>> urlMap = new HashMap<>();
        Map<String, Map<Long, List<BenchmarkResult>>> benchmarksMap = new HashMap<>();
        Map<String, Map<Long, Map<String, Map<String, ResolutionService.CommunityFpsInfo>>>> communityFpsMap = new HashMap<>();
        Map<String, Map<Long, Map<String, Integer>>> submissionCountsMap = new HashMap<>();
        Map<String, Map<Long, GpuPrice>> priceMap = new HashMap<>();

        for (Map.Entry<String, List<GpuModel>> entry : gpusByResolution.entrySet()) {
            String resolution = entry.getKey();
            List<GpuModel> gpus = entry.getValue();

            Map<Long, Double> gpuFpsMap = new HashMap<>();
            Map<Long, String> gpuUrlMap = new HashMap<>();
            Map<Long, List<BenchmarkResult>> gpuBenchmarksMap = new HashMap<>();
            Map<Long, Map<String, Map<String, ResolutionService.CommunityFpsInfo>>> gpuCommunityMap = new HashMap<>();
            Map<Long, Map<String, Integer>> gpuCountsMap = new HashMap<>();


            List<Long> gpuIds = gpus.stream().map(GpuModel::getId).collect(Collectors.toList());
            Map<Long, GpuPrice> gpuPriceMap = priceRepository.findLatestPricesForGpus(gpuIds)
                    .stream()
                    .collect(Collectors.toMap(p -> p.getGpu().getId(), p -> p));

            for (GpuModel gpu : gpus) {
                double avgFps = resolutionService.calculateAverageFps(gpu.getId(), resolution);
                gpuFpsMap.put(gpu.getId(), avgFps);

                String url = resolutionService.getSourceUrl(gpu.getId(), resolution);
                gpuUrlMap.put(gpu.getId(), url);

                List<BenchmarkResult> benchmarks = resolutionService.getBenchmarksForGpuAndResolution(gpu.getId(), resolution);
                gpuBenchmarksMap.put(gpu.getId(), benchmarks);

                Map<String, Map<String, ResolutionService.CommunityFpsInfo>> gameCommunityMap =
                        resolutionService.getCommunityFpsForAllGames(gpu.getId(), resolution, benchmarks);
                gpuCommunityMap.put(gpu.getId(), gameCommunityMap);

                Map<String, Integer> counts = resolutionService.getTotalSubmissionsCountByMode(gpu.getId(), resolution);
                gpuCountsMap.put(gpu.getId(), counts);
            }

            fpsMap.put(resolution, gpuFpsMap);
            urlMap.put(resolution, gpuUrlMap);
            benchmarksMap.put(resolution, gpuBenchmarksMap);
            communityFpsMap.put(resolution, gpuCommunityMap);
            submissionCountsMap.put(resolution, gpuCountsMap);
            priceMap.put(resolution, gpuPriceMap);
        }

        model.addAttribute("gpusByResolution", gpusByResolution);
        model.addAttribute("fpsMap", fpsMap);
        model.addAttribute("urlMap", urlMap);
        model.addAttribute("benchmarksMap", benchmarksMap);
        model.addAttribute("communityFpsMap", communityFpsMap);
        model.addAttribute("submissionCountsMap", submissionCountsMap);
        model.addAttribute("priceMap", priceMap);

        return "resolution";
    }
}