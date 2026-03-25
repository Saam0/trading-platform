package com.example.tradingplatform.service.papertrading;

/**
 * Coordinates one paper trading engine cycle.
 *
 * <p>Each cycle reads market data, evaluates the active strategy
 * and applies broker actions if needed.</p>
 */
public interface PaperTradingOrchestrator {

    /**
     * Processes one trading cycle for the active session.
     */
    void processNextCycle();
}