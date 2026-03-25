package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.StrategyDecision;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.model.PaperPositionSide;
import com.example.tradingplatform.model.TradingSignalAction;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simple test strategy used to verify end-to-end paper trading flow.
 *
 * <p>Behavior:
 * <ul>
 *     <li>If there is no open position -> ENTER_LONG</li>
 *     <li>If there is an open LONG position -> EXIT_LONG</li>
 *     <li>If there is an open SHORT position -> HOLD</li>
 * </ul>
 *
 * <p>This strategy is not intended for real trading.
 * It is only used to verify that market data, orchestrator,
 * broker execution and trade history work correctly together.</p>
 */
@Service
public class OpenCloseTestTradingStrategy implements TradingStrategy {

    /**
     * Returns the unique strategy code used by the resolver.
     *
     * @return strategy code
     */
    @Override
    public String getCode() {
        return "OPEN_CLOSE_TEST";
    }

    /**
     * Evaluates the current account state and alternates between
     * opening and closing a long position.
     *
     * @param candles latest candles loaded for evaluation
     * @param accountState current paper trading account state
     * @return strategy decision
     */
    @Override
    public StrategyDecision evaluate(
            List<Candle> candles,
            PaperTradingAccountState accountState
    ) {
        // open a long position if nothing is currently open
        if (accountState.getOpenPosition() == null) {
            return new StrategyDecision(
                    TradingSignalAction.ENTER_LONG,
                    "Test strategy opens a LONG when no position exists",
                    null,
                    null
            );
        }

        // close the position on the next new candle if the open side is LONG
        if (accountState.getOpenPosition().getSide() == PaperPositionSide.LONG) {
            return new StrategyDecision(
                    TradingSignalAction.EXIT_LONG,
                    "Test strategy closes the LONG on the next candle",
                    null,
                    null
            );
        }

        // safety fallback: do nothing if a SHORT is somehow open
        return StrategyDecision.hold("Test strategy does not manage SHORT positions");
    }
}