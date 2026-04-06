package com.firstproject.framevalue.controller;

import com.firstproject.framevalue.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * PriceController - Displays GPU price changes and trends.
 * Shows current prices, previous prices, and percentage changes.
 */

@Controller
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/price")
    public String showPricePage(Model model) {

        List<PriceService.PriceChangeInfo> priceChanges = priceService.getAllPriceChanges();

        model.addAttribute("priceChanges", priceChanges);

        return "price";
    }
}