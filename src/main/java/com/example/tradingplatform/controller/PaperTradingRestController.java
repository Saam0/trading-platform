package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.PaperTradingStartRequest;
import com.example.tradingplatform.dto.PaperTradingStatusDto;
import com.example.tradingplatform.service.papertrading.PaperTradingService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for paper trading session management.
 */
@RestController
@AllArgsConstructor
public class PaperTradingRestController {

    /** Service responsible for paper trading session lifecycle */
    private final PaperTradingService paperTradingService;

    /**
     * Starts a new paper trading session.
     *
     * @param request start request
     * @return session status
     */
    @PostMapping("/api/paper-trading/start")
    public PaperTradingStatusDto start(@Valid @RequestBody PaperTradingStartRequest request) {
        return paperTradingService.start(request);
    }

    /**
     * Stops the current paper trading session.
     *
     * @return session status
     */
    @PostMapping("/api/paper-trading/stop")
    public PaperTradingStatusDto stop() {
        return paperTradingService.stop();
    }

    /**
     * Returns the current paper trading session status.
     *
     * @return session status
     */
    @GetMapping("/api/paper-trading/status")
    public PaperTradingStatusDto status() {
        return paperTradingService.getStatus();
    }
}