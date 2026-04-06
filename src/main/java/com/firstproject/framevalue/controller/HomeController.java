package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.service.GpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HomeController - Main page displaying GPU comparison table sorted by FPS/₪.
 * Supports filtering by manufacturer and budget (0-20,000 ₪).
 */

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final GpuService gpuService;
    private final GpuPriceRepository priceRepository;

    @GetMapping("/")
    public String homePage(
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) Integer budget,
            @RequestParam(required = false) String sortBy,
            Model model) {


        if (budget != null) {
            budget = Math.max(0, Math.min(budget, 20000));
        }

        List<GpuModel> gpus = gpuService.getFilteredAndSortedGpus(manufacturer, budget, sortBy);

        Map<Long, Double> fpsMap = gpuService.calculateFpsPerShekelForAll(gpus);

        List<Long> gpuIds = gpus.stream().map(GpuModel::getId).collect(Collectors.toList());
        Map<Long, GpuPrice> priceMap = priceRepository.findLatestPricesForGpus(gpuIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getGpu().getId(), p -> p));

        model.addAttribute("gpus", gpus);
        model.addAttribute("fpsMap", fpsMap);
        model.addAttribute("priceMap", priceMap);
        model.addAttribute("manufacturer", manufacturer);
        model.addAttribute("budget", budget);
        model.addAttribute("sortBy", sortBy);

        return "home";
    }
}