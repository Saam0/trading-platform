package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.model.PaperTradingRiskModelType;
import com.example.tradingplatform.service.papertrading.PaperTradingAccountState;

/**
 * Contract for paper trading position size calculation.
 */
public interface PaperTradingPositionSizer {

    /**
     * Returns the supported paper trading risk model.
     *
     * @return supported risk model
     */
    PaperTradingRiskModelType getSupportedRiskModel();

    /**
     * Calculates the notional position size for a new trade.
     *
     * @param accountState current account state
     * @return calculated position size
     */
    double calculatePositionSize(PaperTradingAccountState accountState);
}