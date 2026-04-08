package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingPositionDto;
import com.example.tradingplatform.dto.StrategyDecision;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.model.PaperPositionSide;
import com.example.tradingplatform.model.PaperTradingSessionStatus;
import com.example.tradingplatform.model.TradingSignalAction;
import com.example.tradingplatform.service.papertrading.market.MarketCandleEvent;
import com.example.tradingplatform.service.papertrading.market.MarketDataFeed;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of the paper trading orchestrator.
 *
 * <p>This class is the core engine coordinator for one trading cycle.</p>
 */
@Service
public class PaperTradingOrchestratorImpl implements PaperTradingOrchestrator {

    /** Number of recent candles loaded for strategy evaluation */
    private static final int STRATEGY_CANDLE_LIMIT = 200;

    /** Service that provides access to the active paper trading session */
    private final PaperTradingService paperTradingService;

    /** Market data feed used to fetch latest market information */
    private final MarketDataFeed marketDataFeed;

    /** Strategy resolver used to find the active strategy */
    private final TradingStrategyResolver tradingStrategyResolver;

    /** Broker used to simulate order execution */
    private final PaperBroker paperBroker;

    public PaperTradingOrchestratorImpl(
            PaperTradingService paperTradingService,
            MarketDataFeed marketDataFeed,
            TradingStrategyResolver tradingStrategyResolver,
            PaperBroker paperBroker
    ) {
        this.paperTradingService = paperTradingService;
        this.marketDataFeed = marketDataFeed;
        this.tradingStrategyResolver = tradingStrategyResolver;
        this.paperBroker = paperBroker;
    }

    /**
     * Processes one trading cycle for the active session.
     *
     * <p>The method fetches market data, evaluates the active strategy
     * and applies the resulting decision to the broker/account state.</p>
     */
    @Override
    public void processNextCycle() {
        PaperTradingSession session = paperTradingService.getSession();

        if (session == null || session.getStatus() != PaperTradingSessionStatus.RUNNING) {
            return;
        }

        MarketCandleEvent latestEvent = marketDataFeed.pollLatestClosedCandle(
                session.getTicker(),
                session.getInterval()
        );

        String candleKey = extractCandleKey(latestEvent.getCandle());

        if (candleKey.equals(session.getLastProcessedCandleKey())) {
            return;
        }

        Candle latestCandle = latestEvent.getCandle();

        if (tryCloseByRiskLevels(session, latestCandle)) {
            session.setLastProcessedCandleKey(candleKey);
            return;
        }

        List<Candle> candles = marketDataFeed.getRecentCandles(
                session.getTicker(),
                session.getInterval(),
                STRATEGY_CANDLE_LIMIT
        );

        TradingStrategy strategy = tradingStrategyResolver.resolve(session.getStrategyCode());
        StrategyDecision decision = strategy.evaluate(candles, session.getAccountState());

        applyDecision(session, latestCandle, decision);

        session.setLastProcessedCandleKey(candleKey);
    }

    /**
     * Applies a strategy decision to the current paper trading session.
     *
     * @param session active paper trading session
     * @param latestCandle latest closed candle
     * @param decision strategy output decision
     */
    private void applyDecision(
            PaperTradingSession session,
            Candle latestCandle,
            StrategyDecision decision
    ) {
        double price = latestCandle.getClose();

        if (decision.getAction() == TradingSignalAction.HOLD) {
            session.setLastEventMessage("HOLD: " + safeReason(decision.getReason()));
            return;
        }

        if (decision.getAction() == TradingSignalAction.ENTER_LONG) {
            if (!paperBroker.hasOpenPosition(session.getAccountState())) {
                paperBroker.openLong(
                        session.getAccountState(),
                        price,
                        decision.getSuggestedStopPrice() != null ? decision.getSuggestedStopPrice() : 0.0,
                        decision.getSuggestedTargetPrice() != null ? decision.getSuggestedTargetPrice() : 0.0,
                        decision.getReason()
                );
                session.setLastEventMessage("OPEN_LONG: " + safeReason(decision.getReason()));
            } else {
                session.setLastEventMessage("SKIP_ENTER_LONG_POSITION_ALREADY_OPEN");
            }
            return;
        }

        if (decision.getAction() == TradingSignalAction.ENTER_SHORT) {
            if (!paperBroker.hasOpenPosition(session.getAccountState())) {
                paperBroker.openShort(
                        session.getAccountState(),
                        price,
                        decision.getSuggestedStopPrice() != null ? decision.getSuggestedStopPrice() : 0.0,
                        decision.getSuggestedTargetPrice() != null ? decision.getSuggestedTargetPrice() : 0.0,
                        decision.getReason()
                );
                session.setLastEventMessage("OPEN_SHORT: " + safeReason(decision.getReason()));
            } else {
                session.setLastEventMessage("SKIP_ENTER_SHORT_POSITION_ALREADY_OPEN");
            }
            return;
        }

        if (decision.getAction() == TradingSignalAction.EXIT_LONG) {
            closeOpenPositionIfMatchingSide(
                    session,
                    price,
                    PaperPositionSide.LONG,
                    "SIGNAL",
                    safeReason(decision.getReason())
            );
            return;
        }

        if (decision.getAction() == TradingSignalAction.EXIT_SHORT) {
            closeOpenPositionIfMatchingSide(
                    session,
                    price,
                    PaperPositionSide.SHORT,
                    "SIGNAL",
                    safeReason(decision.getReason())
            );
        }
    }

    /**
     * Tries to close the current open position by stop or target hit.
     *
     * <p>Priority is conservative:
     * stop is checked before target if both are touched within the same candle.</p>
     *
     * @param session active paper trading session
     * @param candle latest closed candle
     * @return true if the position was closed
     */
    private boolean tryCloseByRiskLevels(PaperTradingSession session, Candle candle) {
        if (!paperBroker.hasOpenPosition(session.getAccountState())) {
            return false;
        }

        PaperTradingPositionDto position = session.getAccountState().getOpenPosition();
        PaperPositionSide side = position.getSide();

        double stopPrice = position.getStopPrice();
        double targetPrice = position.getTargetPrice();

        if (side == PaperPositionSide.LONG) {
            if (stopPrice > 0.0 && candle.getLow() <= stopPrice) {
                paperBroker.closePosition(session.getAccountState(), stopPrice, "STOP_LOSS");
                session.setLastEventMessage("CLOSE_LONG_STOP_LOSS");
                return true;
            }

            if (targetPrice > 0.0 && candle.getHigh() >= targetPrice) {
                paperBroker.closePosition(session.getAccountState(), targetPrice, "TAKE_PROFIT");
                session.setLastEventMessage("CLOSE_LONG_TAKE_PROFIT");
                return true;
            }

            return false;
        }

        if (stopPrice > 0.0 && candle.getHigh() >= stopPrice) {
            paperBroker.closePosition(session.getAccountState(), stopPrice, "STOP_LOSS");
            session.setLastEventMessage("CLOSE_SHORT_STOP_LOSS");
            return true;
        }

        if (targetPrice > 0.0 && candle.getLow() <= targetPrice) {
            paperBroker.closePosition(session.getAccountState(), targetPrice, "TAKE_PROFIT");
            session.setLastEventMessage("CLOSE_SHORT_TAKE_PROFIT");
            return true;
        }

        return false;
    }

    /**
     * Closes the open position if it matches the expected side.
     *
     * @param session active paper trading session
     * @param price exit price
     * @param expectedSide side required for closing
     * @param closeSource close source type
     * @param reason close reason text
     */
    private void closeOpenPositionIfMatchingSide(
            PaperTradingSession session,
            double price,
            PaperPositionSide expectedSide,
            String closeSource,
            String reason
    ) {
        if (!paperBroker.hasOpenPosition(session.getAccountState())) {
            session.setLastEventMessage("SKIP_CLOSE_NO_OPEN_POSITION");
            return;
        }

        if (paperBroker.getOpenSide(session.getAccountState()) != expectedSide) {
            session.setLastEventMessage("SKIP_CLOSE_SIDE_MISMATCH_" + expectedSide.name());
            return;
        }

        paperBroker.closePosition(session.getAccountState(), price, reason);
        session.setLastEventMessage("CLOSE_" + expectedSide.name() + "_" + closeSource + ": " + reason);
    }

    /**
     * Returns a non-empty human-readable reason.
     *
     * @param reason raw reason
     * @return normalized reason
     */
    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "NO_REASON";
        }

        return reason;
    }

    /**
     * Extracts a stable unique key for the candle.
     *
     * @param candle market candle
     * @return candle key
     */
    private String extractCandleKey(Candle candle) {
        return String.valueOf(candle.getTime());
    }
}