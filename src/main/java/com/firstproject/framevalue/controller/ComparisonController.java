package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import com.firstproject.framevalue.service.ComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ComparisonController - Compares two GPUs side-by-side by resolution and graphics mode.
 * Shows FPS differences across common games and calculates better value (FPS/₪).
 */

@Controller
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final GpuPriceRepository priceRepository;

    @GetMapping("/comparison")
    public String showComparisonPage(
            @RequestParam(required = false) String resolution,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Long gpu1,
            @RequestParam(required = false) Long gpu2,
            Model model) {

        if (resolution == null) resolution = "1080p";
        if (mode == null) mode = "ultra";

        List<GpuModel> availableGpus = comparisonService.getGpusForResolution(resolution);

        List<Long> gpuIds = availableGpus.stream().map(GpuModel::getId).collect(Collectors.toList());
        Map<Long, GpuPrice> priceMap = priceRepository.findLatestPricesForGpus(gpuIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getGpu().getId(), p -> p));

        model.addAttribute("availableGpus", availableGpus);
        model.addAttribute("priceMap", priceMap);
        model.addAttribute("selectedResolution", resolution);
        model.addAttribute("selectedMode", mode);
        model.addAttribute("selectedGpu1", gpu1);
        model.addAttribute("selectedGpu2", gpu2);

        if (gpu1 != null && gpu2 != null && !gpu1.equals(gpu2)) {
            ComparisonService.ComparisonResult result =
                    comparisonService.compareGpus(gpu1, gpu2, resolution, mode);

            if (result != null) {
                model.addAttribute("comparisonResult", result);
            } else {
                model.addAttribute("error", "No common games found for these GPUs in this resolution and mode");
            }
        }

        return "comparison";
    }
}