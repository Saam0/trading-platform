package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.StrategyDecision;
import com.example.tradingplatform.model.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dummy strategy that never opens or closes a trade.
 *
 * <p>Useful as a safe placeholder while the engine foundation
 * is being built.</p>
 */
@Service
public class NoOpTradingStrategy implements TradingStrategy {

    /**
     * Returns the strategy code used by the resolver.
     *
     * @return strategy code
     */
    @Override
    public String getCode() {
        return "NO_OP";
    }

    /**
     * Always returns HOLD.
     *
     * @param candles latest candles
     * @param accountState current account state
     * @return hold decision
     */
    @Override
    public StrategyDecision evaluate(
            List<Candle> candles,
            PaperTradingAccountState accountState
    ) {
        return StrategyDecision.hold("No-op strategy does not open or close trades");
    }
}