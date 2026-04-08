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

        // do nothing if no active session exists
        if (session == null || session.getStatus() != PaperTradingSessionStatus.RUNNING) {
            return;
        }

        // fetch the latest closed candle event
        MarketCandleEvent latestEvent = marketDataFeed.pollLatestClosedCandle(
                session.getTicker(),
                session.getInterval()
        );

        // build a unique candle key to avoid processing the same candle twice
        String candleKey = extractCandleKey(latestEvent.getCandle());

        // skip the cycle if this candle was already processed
        // do not overwrite the last meaningful event message
        if (candleKey.equals(session.getLastProcessedCandleKey())) {
            return;
        }

        Candle latestCandle = latestEvent.getCandle();

        // first honor risk management exits for already open positions
        if (tryCloseByRiskLevels(session, latestCandle)) {
            session.setLastProcessedCandleKey(candleKey);
            return;
        }

        // load recent candles for strategy evaluation
        List<Candle> candles = marketDataFeed.getRecentCandles(
                session.getTicker(),
                session.getInterval(),
                STRATEGY_CANDLE_LIMIT
        );

        // resolve the active strategy by code
        TradingStrategy strategy = tradingStrategyResolver.resolve(session.getStrategyCode());

        // ask the strategy what action it wants to take
        StrategyDecision decision = strategy.evaluate(candles, session.getAccountState());

        // apply the strategy decision to the broker/account state
        applyDecision(session, latestCandle, decision);

        // remember the processed candle only after the cycle is fully completed
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

        // no action requested by the strategy
        if (decision.getAction() == TradingSignalAction.HOLD) {
            session.setLastEventMessage("HOLD: " + decision.getReason());
            return;
        }

        // open long only if no position is currently open
        if (decision.getAction() == TradingSignalAction.ENTER_LONG) {
            if (!paperBroker.hasOpenPosition(session.getAccountState())) {
                paperBroker.openLong(
                        session.getAccountState(),
                        price,
                        decision.getSuggestedStopPrice() != null ? decision.getSuggestedStopPrice() : 0.0,
                        decision.getSuggestedTargetPrice() != null ? decision.getSuggestedTargetPrice() : 0.0,
                        decision.getReason()
                );
                session.setLastEventMessage("Opened LONG: " + decision.getReason());
            } else {
                session.setLastEventMessage("Skipped ENTER_LONG because a position is already open");
            }
            return;
        }

        // open short only if no position is currently open
        if (decision.getAction() == TradingSignalAction.ENTER_SHORT) {
            if (!paperBroker.hasOpenPosition(session.getAccountState())) {
                paperBroker.openShort(
                        session.getAccountState(),
                        price,
                        decision.getSuggestedStopPrice() != null ? decision.getSuggestedStopPrice() : 0.0,
                        decision.getSuggestedTargetPrice() != null ? decision.getSuggestedTargetPrice() : 0.0,
                        decision.getReason()
                );
                session.setLastEventMessage("Opened SHORT: " + decision.getReason());
            } else {
                session.setLastEventMessage("Skipped ENTER_SHORT because a position is already open");
            }
            return;
        }

        // close long only if the current open side is LONG
        if (decision.getAction() == TradingSignalAction.EXIT_LONG) {
            closeOpenPositionIfMatchingSide(session, price, PaperPositionSide.LONG, decision.getReason());
            return;
        }

        // close short only if the current open side is SHORT
        if (decision.getAction() == TradingSignalAction.EXIT_SHORT) {
            closeOpenPositionIfMatchingSide(session, price, PaperPositionSide.SHORT, decision.getReason());
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
                double exitPrice = resolveExitPrice(candle, stopPrice, true, true);
                paperBroker.closePosition(session.getAccountState(), exitPrice, "STOP_LOSS");
                session.setLastEventMessage("Closed LONG by STOP_LOSS");
                return true;
            }

            if (targetPrice > 0.0 && candle.getHigh() >= targetPrice) {
                double exitPrice = resolveExitPrice(candle, targetPrice, false, true);
                paperBroker.closePosition(session.getAccountState(), exitPrice, "TAKE_PROFIT");
                session.setLastEventMessage("Closed LONG by TAKE_PROFIT");
                return true;
            }

            return false;
        }

        if (stopPrice > 0.0 && candle.getHigh() >= stopPrice) {
            double exitPrice = resolveExitPrice(candle, stopPrice, true, false);
            paperBroker.closePosition(session.getAccountState(), exitPrice, "STOP_LOSS");
            session.setLastEventMessage("Closed SHORT by STOP_LOSS");
            return true;
        }

        if (targetPrice > 0.0 && candle.getLow() <= targetPrice) {
            double exitPrice = resolveExitPrice(candle, targetPrice, false, false);
            paperBroker.closePosition(session.getAccountState(), exitPrice, "TAKE_PROFIT");
            session.setLastEventMessage("Closed SHORT by TAKE_PROFIT");
            return true;
        }

        return false;
    }

    /**
     * Resolves exit price for candle-based stop/target execution.
     *
     * <p>In this phase we use the planned stop/target level itself as the fill price
     * once the candle proves that level was touched.</p>
     *
     * @param candle latest closed candle
     * @param level stop or target level
     * @param stopExit whether the exit is by stop
     * @param longSide whether the position is long
     * @return resolved exit price
     */
    private double resolveExitPrice(
            Candle candle,
            double level,
            boolean stopExit,
            boolean longSide
    ) {
        return level;
    }

    /**
     * Closes the open position if it matches the expected side.
     *
     * @param session active paper trading session
     * @param price exit price
     * @param expectedSide side required for closing
     * @param reason close reason
     */
    private void closeOpenPositionIfMatchingSide(
            PaperTradingSession session,
            double price,
            PaperPositionSide expectedSide,
            String reason
    ) {
        if (!paperBroker.hasOpenPosition(session.getAccountState())) {
            session.setLastEventMessage("Skipped close because no open position exists");
            return;
        }

        if (paperBroker.getOpenSide(session.getAccountState()) != expectedSide) {
            session.setLastEventMessage("Skipped close because open side does not match " + expectedSide);
            return;
        }

        paperBroker.closePosition(session.getAccountState(), price, reason);
        session.setLastEventMessage("Closed " + expectedSide + ": " + reason);
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