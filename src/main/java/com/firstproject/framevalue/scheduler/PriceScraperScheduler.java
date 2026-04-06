package com.firstproject.framevalue.scheduler;

import com.firstproject.framevalue.service.PriceScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PriceScraperScheduler - Updates GPU prices from Israeli retailers.
 * Runs every Saturday at midnight using Python Playwright scraper.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceScraperScheduler {

    private final PriceScraperService scraperService;



    @Scheduled(cron = "0 0 0 * * SAT")  // Every Saturday at midnight
    //@Scheduled(cron = "0 */2 * * * *")   // Every 2 minutes (for testing)
    public void schedulePriceScraping() {
        log.info("Starting scheduled price scraping");
        scraperService.scrapePrices();
    }
}