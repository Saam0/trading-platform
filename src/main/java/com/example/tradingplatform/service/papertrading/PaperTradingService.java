package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingStartRequest;
import com.example.tradingplatform.dto.PaperTradingStatusDto;

/**
 * Service contract for paper trading session management.
 */
public interface PaperTradingService {

    /**
     * Starts a paper trading session.
     *
     * @param request start request
     * @return session status
     */
    PaperTradingStatusDto start(PaperTradingStartRequest request);

    /**
     * Stops the active paper trading session.
     *
     * @return session status
     */
    PaperTradingStatusDto stop();

    /**
     * Returns current session status.
     *
     * @return session status
     */
    PaperTradingStatusDto getStatus();

    /**
     * Returns the current in-memory paper trading session.
     *
     * @return active session
     */
    PaperTradingSession getSession();
}