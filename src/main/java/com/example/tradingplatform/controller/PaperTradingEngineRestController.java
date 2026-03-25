package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.PaperTradingStatusDto;
import com.example.tradingplatform.service.papertrading.PaperTradingOrchestrator;
import com.example.tradingplatform.service.papertrading.PaperTradingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for manual paper trading engine actions.
 *
 * <p>This controller is used to manually trigger one engine cycle
 * before scheduler-based automation is added.</p>
 */
@RestController
@AllArgsConstructor
public class PaperTradingEngineRestController {

    /** Orchestrator responsible for processing one trading cycle */
    private final PaperTradingOrchestrator paperTradingOrchestrator;

    /** Service used to return updated session status after the cycle */
    private final PaperTradingService paperTradingService;

    /**
     * Runs one manual paper trading cycle and returns updated status.
     *
     * @return updated paper trading session status
     */
    @PostMapping("/api/paper-trading/engine/next-cycle")
    public PaperTradingStatusDto runNextCycle() {
        // process one market -> strategy -> broker cycle
        paperTradingOrchestrator.processNextCycle();

        // return the latest session snapshot after processing
        return paperTradingService.getStatus();
    }
}