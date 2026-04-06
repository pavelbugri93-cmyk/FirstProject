package com.firstproject.framevalue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstproject.framevalue.entity.GpuModel;
import com.firstproject.framevalue.entity.GpuPrice;
import com.firstproject.framevalue.repository.GpuModelRepository;
import com.firstproject.framevalue.repository.GpuPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PriceScraperService - Executes Python Playwright scraper and updates GPU prices.
 * Reads scraped data from JSON, matches products to database, and tracks price changes.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceScraperService {

    private final GpuPriceRepository priceRepository;
    private final GpuModelRepository gpuRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void scrapePrices() {
        try {
            log.info("Starting price scraping...");

            if (!runPythonScraper()) {
                log.error("Scraper script failed!");
                return;
            }

            List<Map<String, Object>> prices = readPricesFromJson();
            if (prices == null) {
                log.error("Failed to read JSON file!");
                return;
            }

            log.info("Found {} prices", prices.size());

            int updated = 0, created = 0;

            for (Map<String, Object> item : prices) {
                String productName = (String) item.get("product");
                Integer price = (Integer) item.get("price");
                String link = (String) item.get("link");

                if (price == 0) {
                    log.warn("{} - no price", productName);
                    continue;
                }

                GpuModel gpu = findGpuByName(productName);
                if (gpu == null) {
                    log.warn("{} - not found in database", productName);
                    continue;
                }

                if (updateOrCreatePrice(gpu, price, link)) {
                    updated++;
                } else {
                    created++;
                }
            }

            log.info("Completed! Updated: {} | Created: {}", updated, created);

        } catch (Exception e) {
            log.error("Error scraping prices", e);
        }
    }

    private boolean runPythonScraper() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "src/main/resources/scripts/tms.py");
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Python: {}", line);
            }

            return process.waitFor() == 0;

        } catch (Exception e) {
            log.error("Error running Python script", e);
            return false;
        }
    }

    private List<Map<String, Object>> readPricesFromJson() {
        try {
            File jsonFile = new File("pc_prices.json");
            if (!jsonFile.exists()) {
                log.error("File pc_prices.json does not exist!");
                return null;
            }

            return objectMapper.readValue(jsonFile, new TypeReference<List<Map<String, Object>>>() {});

        } catch (Exception e) {
            log.error("Error reading JSON", e);
            return null;
        }
    }

    private GpuModel findGpuByName(String productName) {
        Optional<GpuModel> exactMatch = gpuRepository.findByModelNameIgnoreCase(productName);
        if (exactMatch.isPresent()) {
            log.info("{} - found!", productName);
            return exactMatch.get();
        }

        List<GpuModel> partialMatches = gpuRepository.findByModelNameContaining(productName);
        if (!partialMatches.isEmpty()) {
            GpuModel gpu = partialMatches.get(0);
            log.warn("{} → using {} (found {} matches)",
                    productName, gpu.getModelName(), partialMatches.size());
            return gpu;
        }

        return null;
    }

    private boolean updateOrCreatePrice(GpuModel gpu, Integer newPriceValue, String link) {
        BigDecimal newPrice = BigDecimal.valueOf(newPriceValue);

        Optional<GpuPrice> existingOpt = priceRepository.findTopByGpuIdOrderByUpdatedAtDesc(gpu.getId());

        if (existingOpt.isPresent()) {
            return updateExistingPrice(existingOpt.get(), newPrice, link, gpu.getModelName());
        } else {
            createNewPrice(gpu, newPrice, link);
            return false;
        }
    }


    private boolean updateExistingPrice(GpuPrice existing, BigDecimal newPrice, String link, String gpuName) {
        BigDecimal oldPrice = existing.getCurrentPrice();

        if (oldPrice.equals(newPrice)) {
            log.info("{} - same price ({}₪)", gpuName, oldPrice);
            return false;
        }

        existing.setPreviousPrice(oldPrice);

        existing.setCurrentPrice(newPrice);

        double changePercent = ((newPrice.doubleValue() - oldPrice.doubleValue()) / oldPrice.doubleValue()) * 100;
        existing.setPriceChangePercent(changePercent);
        existing.setIsraelProductUrl(link);

        priceRepository.save(existing);

        log.info("{} - updated: {}₪ → {}₪ ({}{:.1f}%)",
                gpuName, oldPrice, newPrice,
                changePercent > 0 ? "+" : "",
                changePercent);

        return true;
    }

    private void createNewPrice(GpuModel gpu, BigDecimal price, String link) {
        GpuPrice newPrice = GpuPrice.builder()
                .gpu(gpu)
                .currentPrice(price)
                .previousPrice(null)
                .priceChangePercent(0.0)
                .israelProductUrl(link)
                .sourceUrl(link)
                .build();

        priceRepository.save(newPrice);
        log.info("{} - created new: {}₪", gpu.getModelName(), price);
    }
}