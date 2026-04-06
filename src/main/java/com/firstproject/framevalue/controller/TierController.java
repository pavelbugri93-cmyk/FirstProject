package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.service.TierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TierController - Groups GPUs by performance tier (1-5) and compares AMD vs NVIDIA.
 * Determines the better value brand per tier based on FPS/₪ ratio.
 */

@Controller
@RequiredArgsConstructor
public class TierController {

    private final TierService tierService;
    private final GpuPriceRepository priceRepository;

    @GetMapping("/tier")
    public String showTierPage(
            @RequestParam(required = false, defaultValue = "1") Integer tier,
            Model model) {

        TierService.TierData tierData = tierService.getTierData(tier);
        TierService.WinnerInfo winner = tierService.determineWinner(tierData);

        List<Long> gpuIds = new ArrayList<>();
        tierData.getAmdGpus().forEach(gpu -> gpuIds.add(gpu.getId()));
        tierData.getNvidiaGpus().forEach(gpu -> gpuIds.add(gpu.getId()));

        Map<Long, GpuPrice> priceMap = priceRepository.findLatestPricesForGpus(gpuIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getGpu().getId(), p -> p));

        model.addAttribute("selectedTier", tier);
        model.addAttribute("tierData", tierData);
        model.addAttribute("winner", winner);
        model.addAttribute("priceMap", priceMap);

        return "tier";
    }
}