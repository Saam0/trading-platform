package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.StrategyDecision;
import com.example.tradingplatform.model.Candle;

import java.util.List;

/**
 * Generic contract for all live/paper trading strategies.
 *
 * <p>A strategy analyzes market data and current account state
 * and returns a decision for the trading engine.</p>
 */
public interface TradingStrategy {

    /**
     * Returns the unique strategy code used for resolution.
     *
     * @return strategy code
     */
    String getCode();

    /**
     * Evaluates the current market/account context and returns a decision.
     *
     * @param candles latest candle data
     * @param accountState current paper trading account state
     * @return strategy decision
     */
    StrategyDecision evaluate(
            List<Candle> candles,
            PaperTradingAccountState accountState
    );
}