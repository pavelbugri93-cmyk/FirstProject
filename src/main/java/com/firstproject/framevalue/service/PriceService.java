package com.firstproject.framevalue.service;

import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PriceService - Provides GPU price change data for the price tracking page.
 * Calculates price differences and percentage changes with optimized batch queries.
 */

@Service
@RequiredArgsConstructor
public class PriceService {

    private final GpuModelRepository gpuRepository;
    private final GpuPriceRepository priceRepository;

    public List<PriceChangeInfo> getAllPriceChanges() {
        List<GpuModel> allGpus = gpuRepository.findAll();

        List<GpuPrice> allPrices = priceRepository.findAll();

        Map<Long, GpuPrice> priceMap = allPrices.stream()
                .collect(Collectors.toMap(
                        price -> price.getGpu().getId(),
                        price -> price,
                        (existing, replacement) -> existing
                ));

        return allGpus.stream()
                .map(gpu -> createPriceChangeInfo(gpu, priceMap.get(gpu.getId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PriceChangeInfo::getModelName))
                .collect(Collectors.toList());
    }

    private PriceChangeInfo createPriceChangeInfo(GpuModel gpu, GpuPrice price) {
        if (price == null) {
            return null;
        }

        BigDecimal currentPrice = price.getCurrentPrice();
        BigDecimal previousPrice = price.getPreviousPrice();

        BigDecimal change = BigDecimal.ZERO;
        String changeDirection = "=";
        String changeColor = "secondary";

        if (previousPrice != null && currentPrice != null) {
            change = currentPrice.subtract(previousPrice);

            if (change.compareTo(BigDecimal.ZERO) > 0) {
                changeDirection = "↑";
                changeColor = "danger";
            } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                changeDirection = "↓";
                changeColor = "success";
                change = change.abs();
            }
        }

        return new PriceChangeInfo(
                gpu.getModelName(),
                gpu.getManufacturer(),
                currentPrice,
                previousPrice,
                change,
                changeDirection,
                changeColor
        );
    }

    @Getter
    @AllArgsConstructor
    public static class PriceChangeInfo {
        private String modelName;
        private String manufacturer;
        private BigDecimal currentPrice;
        private BigDecimal previousPrice;
        private BigDecimal change;
        private String changeDirection;
        private String changeColor;
    }
}