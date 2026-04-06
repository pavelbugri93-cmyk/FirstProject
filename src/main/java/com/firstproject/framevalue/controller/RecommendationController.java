package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.service.RecommendationService;
import com.firstproject.framevalue.service.RecommendationService.RecommendationResult;
import com.firstproject.framevalue.repository.BenchmarkResultRepository;
import com.firstproject.framevalue.repository.GpuModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RecommendationController - Recommends best GPUs based on budget, resolution, and selected games.
 * Ranks GPUs by match score (FPS performance + price-to-performance ratio).
 */

@Controller
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final BenchmarkResultRepository benchmarkResultRepository;
    private final GpuModelRepository gpuModelRepository; // ← חדש!

    @GetMapping("/recommendation")
    public String showRecommendationPage(
            @RequestParam(required = false) Integer budget,
            @RequestParam(required = false) String resolution,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) List<String> games,
            Model model) {

        List<String> availableGames = benchmarkResultRepository.findAll()
                .stream()
                .map(benchmark -> benchmark.getGameName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        model.addAttribute("availableGames", availableGames);

        if (budget != null && resolution != null && mode != null && games != null && !games.isEmpty()) {


            RecommendationService.RecommendationResponse response =
                    recommendationService.getRecommendations(budget, resolution, mode, games);

            model.addAttribute("recommendations", response.getRecommendations());
            model.addAttribute("testedGpus", response.getTotalTested());
            model.addAttribute("totalGpus", response.getTotalGpus());
            model.addAttribute("budget", budget);
            model.addAttribute("resolution", resolution);
            model.addAttribute("mode", mode);
            model.addAttribute("selectedGames", games);
        }

        return "recommendation";
    }
}