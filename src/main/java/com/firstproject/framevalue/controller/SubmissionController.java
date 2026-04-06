package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SubmissionController - Handles community FPS reports from users.
 * Enforces 3 reports/day/game limit and validates submissions (max 40% deviation).
 */

@Controller
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/submission")
    public String showSubmissionPage(
            @RequestParam(required = false) Long gpuId,
            @RequestParam(required = false) String game,
            HttpServletRequest request,
            Model model) {

        String userIp = request.getRemoteAddr();

        List<GpuModel> gpus = submissionService.getAllGpus();
        model.addAttribute("gpus", gpus);
        model.addAttribute("selectedGpuId", gpuId);
        model.addAttribute("selectedGame", game);

        if (gpuId != null) {
            List<String> games = submissionService.getGamesForGpu(gpuId);
            model.addAttribute("games", games);

            if (game != null && !game.isEmpty()) {
                SubmissionService.GameModeInfo modeInfo = submissionService.getGameModeInfo(gpuId, game, userIp);  // ← העבר IP
                model.addAttribute("modeInfo", modeInfo);

                String autoResolution = submissionService.getResolutionForGame(gpuId, game);
                model.addAttribute("autoResolution", autoResolution);
            }
        }

        return "submission";
    }

    @PostMapping("/submission/submit")
    public String submitFps(
            @RequestParam Long gpuId,
            @RequestParam String game,
            @RequestParam String mode,
            @RequestParam String resolution,
            @RequestParam int fps,
            HttpServletRequest request,
            Model model) {

        String userIp = request.getRemoteAddr();

        if (submissionService.hasReachedDailyLimitForGame(userIp, gpuId, game)) {
            model.addAttribute("error", "You've reached the limit of 3 reports per day for " + game + ". Try again tomorrow!");

            List<GpuModel> gpus = submissionService.getAllGpus();
            model.addAttribute("gpus", gpus);

            return "submission";
        }

        SubmissionService.ValidationResult validation =
                submissionService.validateSubmission(gpuId, game, mode, fps);

        if (!validation.isValid()) {
            model.addAttribute("error", validation.getMessage());

            List<GpuModel> gpus = submissionService.getAllGpus();
            List<String> games = submissionService.getGamesForGpu(gpuId);
            SubmissionService.GameModeInfo modeInfo = submissionService.getGameModeInfo(gpuId, game, userIp);
            String autoResolution = submissionService.getResolutionForGame(gpuId, game);

            model.addAttribute("gpus", gpus);
            model.addAttribute("games", games);
            model.addAttribute("modeInfo", modeInfo);
            model.addAttribute("autoResolution", autoResolution);
            model.addAttribute("selectedGpuId", gpuId);
            model.addAttribute("selectedGame", game);

            return "submission";
        }

        submissionService.saveSubmission(gpuId, game, mode, resolution, fps, userIp);

        model.addAttribute("success", "Report submitted successfully! Thanks for contributing to the community");

        List<GpuModel> gpus = submissionService.getAllGpus();
        model.addAttribute("gpus", gpus);

        return "submission";
    }

    @GetMapping("/submission/view-reports")
    public String viewReports(
            @RequestParam Long gpuId,
            @RequestParam String game,
            @RequestParam String mode,
            Model model) {

        List<SubmissionService.UserSubmissionDTO> reports =
                submissionService.getSubmissionsForGameAndMode(gpuId, game, mode);

        int totalReports = reports.size();

        if (!reports.isEmpty()) {
            double average = reports.stream()
                    .mapToInt(SubmissionService.UserSubmissionDTO::getFps)
                    .average()
                    .orElse(0);

            int max = reports.stream()
                    .mapToInt(SubmissionService.UserSubmissionDTO::getFps)
                    .max()
                    .orElse(0);

            int min = reports.stream()
                    .mapToInt(SubmissionService.UserSubmissionDTO::getFps)
                    .min()
                    .orElse(0);

            model.addAttribute("avgFps", Math.round(average));
            model.addAttribute("maxFps", max);
            model.addAttribute("minFps", min);
        }

        model.addAttribute("reports", reports);
        model.addAttribute("gpuId", gpuId);
        model.addAttribute("game", game);
        model.addAttribute("mode", mode);
        model.addAttribute("totalReports", totalReports);

        return "view-reports";
    }

    @GetMapping("/submission/get-reports")
    @ResponseBody
    public Map<String, Object> getReports(
            @RequestParam Long gpuId,
            @RequestParam String game,
            @RequestParam String mode) {

        List<SubmissionService.UserSubmissionDTO> reports =
                submissionService.getSubmissionsForGameAndMode(gpuId, game, mode);

        Map<String, Object> response = new HashMap<>();
        response.put("reports", reports);
        response.put("total", reports.size());

        if (!reports.isEmpty()) {
            double average = reports.stream().mapToInt(SubmissionService.UserSubmissionDTO::getFps).average().orElse(0);
            int max = reports.stream().mapToInt(SubmissionService.UserSubmissionDTO::getFps).max().orElse(0);
            int min = reports.stream().mapToInt(SubmissionService.UserSubmissionDTO::getFps).min().orElse(0);

            response.put("average", Math.round(average));
            response.put("max", max);
            response.put("min", min);
        }

        return response;
    }

}