package com.example.tradingplatform.service.papertrading;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background scheduler that periodically triggers one paper trading engine cycle.
 *
 * <p>The scheduler uses a fixed delay for now. Duplicate candle protection
 * ensures that the same candle is not processed multiple times.</p>
 */
@Component
public class PaperTradingScheduler {

    /** Orchestrator responsible for one market -> strategy -> broker cycle */
    private final PaperTradingOrchestrator paperTradingOrchestrator;

    public PaperTradingScheduler(PaperTradingOrchestrator paperTradingOrchestrator) {
        this.paperTradingOrchestrator = paperTradingOrchestrator;
    }

    /**
     * Runs one scheduled paper trading cycle.
     *
     * <p>Fixed delay is used as a simple first version.
     * More advanced interval-aware scheduling can be added later.</p>
     */
    @Scheduled(fixedDelay = 5000)
    public void runPaperTradingCycle() {
        // delegate one cycle to the orchestrator
        paperTradingOrchestrator.processNextCycle();
    }
}