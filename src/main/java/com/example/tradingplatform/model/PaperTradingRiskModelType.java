package com.example.tradingplatform.model;

/**
 * Represents the position sizing model used in paper trading.
 */
public enum PaperTradingRiskModelType {

    /** Uses full available balance with leverage */
    FULL_BALANCE,

    /**
     * Uses stop distance and risk percent to calculate position size.
     *
     * <p>This model is intended for scenarios where the user defines
     * an entry price, a stop price and an allowed risk percent.</p>
     */
    STOP_RISK_PERCENT
}