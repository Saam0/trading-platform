package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingPositionDto;
import com.example.tradingplatform.dto.PaperTradingStartRequest;
import com.example.tradingplatform.dto.PaperTradingStatusDto;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.PaperPositionSide;
import com.example.tradingplatform.model.PaperTradingSessionStatus;
import com.example.tradingplatform.service.papertrading.market.MarketCandleEvent;
import com.example.tradingplatform.service.papertrading.market.MarketDataFeed;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Default implementation of PaperTradingService.
 *
 * <p>This service manages the lifecycle of the in-memory paper trading session.</p>
 */
@Service
public class PaperTradingServiceImpl implements PaperTradingService {

    /** Strategy resolver used to validate/resolve strategy codes */
    private final TradingStrategyResolver tradingStrategyResolver;

    /** Broker used for simulated execution */
    private final PaperBroker paperBroker;

    /** Market data feed used to resolve current market price for status view */
    private final MarketDataFeed marketDataFeed;

    /** Single in-memory session state for the current phase */
    private final PaperTradingSession session = new PaperTradingSession();

    /**
     * Creates the service with required collaborators.
     *
     * @param tradingStrategyResolver strategy resolver
     * @param paperBroker broker implementation
     * @param marketDataFeed market data feed
     */
    public PaperTradingServiceImpl(
            TradingStrategyResolver tradingStrategyResolver,
            PaperBroker paperBroker,
            MarketDataFeed marketDataFeed
    ) {
        this.tradingStrategyResolver = tradingStrategyResolver;
        this.paperBroker = paperBroker;
        this.marketDataFeed = marketDataFeed;
    }

    /**
     * Starts a new paper trading session.
     *
     * @param request start request
     * @return current status dto
     */
    @Override
    public synchronized PaperTradingStatusDto start(PaperTradingStartRequest request) {
        // validate that the requested strategy exists
        tradingStrategyResolver.resolve(request.getStrategyCode());

        PaperTradingAccountState accountState = new PaperTradingAccountState();
        accountState.setInitialBalance(request.getInitialBalance());
        accountState.setCurrentBalance(request.getInitialBalance());
        accountState.setFeePercent(request.getFeePercent());
        accountState.setLeverage(request.getLeverage());

        // store the selected sizing/risk model configuration in account state
        accountState.setRiskModelType(request.getRiskModelType());
        accountState.setRiskPercent(request.getRiskPercent());

        // store optional planned prices for stop-based sizing models
        accountState.setPlannedEntryPrice(request.getPlannedEntryPrice());
        accountState.setPlannedStopPrice(request.getPlannedStopPrice());

        // store exit model configuration
        accountState.setExitModelType(request.getExitModelType());
        accountState.setRiskRewardRatio(request.getRiskRewardRatio());

        accountState.setClosedTrades(new ArrayList<>());

        // initialize the session metadata
        session.setStatus(PaperTradingSessionStatus.RUNNING);
        session.setTicker(request.getTicker());
        session.setInterval(request.getInterval());
        session.setStrategyCode(request.getStrategyCode().toUpperCase());
        session.setAccountState(accountState);
        session.setLastProcessedCandleKey(null);
        session.setLastEventMessage("Paper trading session started");

        return toStatusDto();
    }

    /**
     * Stops the current paper trading session.
     *
     * @return current status dto
     */
    @Override
    public synchronized PaperTradingStatusDto stop() {
        if (session.getStatus() == PaperTradingSessionStatus.STOPPED) {
            throw new InvalidRequestException("Paper trading session is already stopped");
        }

        session.setStatus(PaperTradingSessionStatus.STOPPED);
        session.setLastEventMessage("Paper trading session stopped");

        return toStatusDto();
    }

    /**
     * Returns the current paper trading session status.
     *
     * @return status dto
     */
    @Override
    public synchronized PaperTradingStatusDto getStatus() {
        return toStatusDto();
    }

    /**
     * Returns the internal in-memory session.
     *
     * @return current session
     */
    @Override
    public synchronized PaperTradingSession getSession() {
        return session;
    }

    /**
     * Maps the internal session state to a status dto.
     *
     * @return mapped status dto
     */
    private PaperTradingStatusDto toStatusDto() {
        if (session.getAccountState() == null) {
            return new PaperTradingStatusDto(
                    session.getStatus(),
                    session.getTicker(),
                    session.getInterval(),
                    session.getStrategyCode(),
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    null,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    null,
                    new ArrayList<>(),
                    session.getLastEventMessage()
            );
        }

        Double currentPrice = resolveCurrentPrice();
        double unrealizedPnl = calculateUnrealizedPnl(session.getAccountState().getOpenPosition(), currentPrice);
        double unrealizedPnlPercent = calculateUnrealizedPnlPercent(
                session.getAccountState().getOpenPosition(),
                unrealizedPnl
        );
        double equity = session.getAccountState().getCurrentBalance() + unrealizedPnl;

        // calculate total pnl relative to initial balance
        double totalPnl = session.getAccountState().getCurrentBalance()
                - session.getAccountState().getInitialBalance();

        // calculate total pnl percent
        double totalPnlPercent = session.getAccountState().getInitialBalance() == 0.0
                ? 0.0
                : (totalPnl * 100.0) / session.getAccountState().getInitialBalance();

        // calculate required leverage for the current open position
        double requiredLeverage = calculateRequiredLeverage(
                session.getAccountState().getOpenPosition(),
                session.getAccountState().getCurrentBalance()
        );

        return new PaperTradingStatusDto(
                session.getStatus(),
                session.getTicker(),
                session.getInterval(),
                session.getStrategyCode(),
                session.getAccountState().getInitialBalance(),
                session.getAccountState().getCurrentBalance(),
                session.getAccountState().getFeePercent(),
                session.getAccountState().getLeverage(),
                currentPrice,
                unrealizedPnl,
                unrealizedPnlPercent,
                equity,
                totalPnl,
                totalPnlPercent,
                requiredLeverage,
                session.getAccountState().getOpenPosition(),
                session.getAccountState().getClosedTrades(),
                session.getLastEventMessage()
        );
    }

    /**
     * Resolves the current market price from the latest closed candle.
     *
     * @return current close price or null if session is not active enough
     */
    private Double resolveCurrentPrice() {
        if (session.getTicker() == null || session.getInterval() == null) {
            return null;
        }

        try {
            MarketCandleEvent latestEvent = marketDataFeed.pollLatestClosedCandle(
                    session.getTicker(),
                    session.getInterval()
            );
            return latestEvent.getCandle().getClose();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Calculates unrealized pnl for the currently open position.
     *
     * @param openPosition current open position
     * @param currentPrice latest market price
     * @return unrealized pnl
     */
    private double calculateUnrealizedPnl(
            PaperTradingPositionDto openPosition,
            Double currentPrice
    ) {
        if (openPosition == null || currentPrice == null) {
            return 0.0;
        }

        if (openPosition.getSide() == PaperPositionSide.LONG) {
            return (currentPrice - openPosition.getEntryPrice()) * openPosition.getQuantity();
        }

        return (openPosition.getEntryPrice() - currentPrice) * openPosition.getQuantity();
    }

    /**
     * Calculates unrealized pnl percent relative to position size.
     *
     * @param openPosition current open position
     * @param unrealizedPnl unrealized pnl value
     * @return unrealized pnl percent
     */
    private double calculateUnrealizedPnlPercent(
            PaperTradingPositionDto openPosition,
            double unrealizedPnl
    ) {
        if (openPosition == null || openPosition.getPositionSize() == 0.0) {
            return 0.0;
        }

        return (unrealizedPnl * 100.0) / openPosition.getPositionSize();
    }

    /**
     * Calculates the leverage required for the current open position.
     *
     * @param openPosition current open position
     * @param currentBalance current realized account balance
     * @return required leverage
     */
    private double calculateRequiredLeverage(
            PaperTradingPositionDto openPosition,
            double currentBalance
    ) {
        if (openPosition == null || currentBalance <= 0.0) {
            return 0.0;
        }

        return openPosition.getPositionSize() / currentBalance;
    }
}