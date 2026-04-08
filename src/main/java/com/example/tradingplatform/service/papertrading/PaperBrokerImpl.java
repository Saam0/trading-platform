package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingPositionDto;
import com.example.tradingplatform.dto.PaperTradingTradeDto;
import com.example.tradingplatform.model.PaperPositionSide;
import com.example.tradingplatform.service.papertrading.risk.PaperTradingPositionSizer;
import com.example.tradingplatform.service.papertrading.risk.PaperTradingPositionSizerResolver;
import org.springframework.stereotype.Service;

/**
 * In-memory fake broker used for paper trading.
 *
 * <p>This implementation does not connect to a real exchange.
 * It only updates the local account state.</p>
 */
@Service
public class PaperBrokerImpl implements PaperBroker {

    /** Resolver used to select the active position sizing model */
    private final PaperTradingPositionSizerResolver positionSizerResolver;

    /** Service used to resolve trade target price based on exit model */
    private final TradePlanService tradePlanService;

    /**
     * Creates the paper broker with required collaborators.
     *
     * @param positionSizerResolver resolver for position sizing models
     * @param tradePlanService service for stop/target trade planning
     */
    public PaperBrokerImpl(
            PaperTradingPositionSizerResolver positionSizerResolver,
            TradePlanService tradePlanService
    ) {
        this.positionSizerResolver = positionSizerResolver;
        this.tradePlanService = tradePlanService;
    }

    /**
     * Opens a long position using the configured position sizing model.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price hint
     * @param reason open reason
     * @return created position
     */
    @Override
    public PaperTradingPositionDto openLong(
            PaperTradingAccountState state,
            double price,
            double stopPrice,
            double targetPrice,
            String reason
    ) {
        if (hasOpenPosition(state)) {
            throw new IllegalStateException("Cannot open LONG because a position is already open");
        }

        double resolvedStopPrice = resolveStopPrice(state, stopPrice, price);

        updatePlannedPricesForSizing(state, price, resolvedStopPrice);

        double positionSize = calculatePositionSize(state);
        double quantity = positionSize / price;

        double resolvedTargetPrice = resolveTargetPrice(
                state,
                price,
                resolvedStopPrice,
                targetPrice,
                PaperPositionSide.LONG
        );

        PaperTradingPositionDto position = new PaperTradingPositionDto(
                PaperPositionSide.LONG,
                price,
                quantity,
                positionSize,
                resolvedStopPrice,
                resolvedTargetPrice
        );

        state.setOpenPosition(position);

        return position;
    }

    /**
     * Opens a short position using the configured position sizing model.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price hint
     * @param reason open reason
     * @return created position
     */
    @Override
    public PaperTradingPositionDto openShort(
            PaperTradingAccountState state,
            double price,
            double stopPrice,
            double targetPrice,
            String reason
    ) {
        if (hasOpenPosition(state)) {
            throw new IllegalStateException("Cannot open SHORT because a position is already open");
        }

        double resolvedStopPrice = resolveStopPrice(state, stopPrice, price);

        updatePlannedPricesForSizing(state, price, resolvedStopPrice);

        double positionSize = calculatePositionSize(state);
        double quantity = positionSize / price;

        double resolvedTargetPrice = resolveTargetPrice(
                state,
                price,
                resolvedStopPrice,
                targetPrice,
                PaperPositionSide.SHORT
        );

        PaperTradingPositionDto position = new PaperTradingPositionDto(
                PaperPositionSide.SHORT,
                price,
                quantity,
                positionSize,
                resolvedStopPrice,
                resolvedTargetPrice
        );

        state.setOpenPosition(position);

        return position;
    }

    /**
     * Closes the currently open position and realizes PnL.
     *
     * @param state current account state
     * @param price exit price
     * @param reason close reason
     * @return closed trade record
     */
    @Override
    public PaperTradingTradeDto closePosition(
            PaperTradingAccountState state,
            double price,
            String reason
    ) {
        if (!hasOpenPosition(state)) {
            throw new IllegalStateException("Cannot close position because no open position exists");
        }

        PaperTradingPositionDto position = state.getOpenPosition();

        double grossPnl = calculatePnl(position, price);
        double fee = calculateFee(position.getPositionSize(), state.getFeePercent());
        double netPnl = grossPnl - fee;

        state.setCurrentBalance(state.getCurrentBalance() + netPnl);

        PaperTradingTradeDto trade = new PaperTradingTradeDto(
                position.getSide(),
                position.getEntryPrice(),
                price,
                position.getQuantity(),
                position.getPositionSize(),
                fee,
                netPnl,
                reason
        );

        state.getClosedTrades().add(trade);
        state.setOpenPosition(null);

        return trade;
    }

    /**
     * Checks whether an open position exists.
     *
     * @param state current account state
     * @return true if an open position exists
     */
    @Override
    public boolean hasOpenPosition(PaperTradingAccountState state) {
        return state.getOpenPosition() != null;
    }

    /**
     * Checks whether the current open position is long.
     *
     * @param state current account state
     * @return true if open long exists
     */
    @Override
    public boolean hasLongPosition(PaperTradingAccountState state) {
        return hasOpenPosition(state) && state.getOpenPosition().getSide() == PaperPositionSide.LONG;
    }

    /**
     * Checks whether the current open position is short.
     *
     * @param state current account state
     * @return true if open short exists
     */
    @Override
    public boolean hasShortPosition(PaperTradingAccountState state) {
        return hasOpenPosition(state) && state.getOpenPosition().getSide() == PaperPositionSide.SHORT;
    }

    /**
     * Returns the open position side.
     *
     * @param state current account state
     * @return open side or null
     */
    @Override
    public PaperPositionSide getOpenSide(PaperTradingAccountState state) {
        return hasOpenPosition(state) ? state.getOpenPosition().getSide() : null;
    }

    /**
     * Calculates position size using the configured risk model.
     *
     * @param state current account state
     * @return calculated position size
     */
    private double calculatePositionSize(PaperTradingAccountState state) {
        PaperTradingPositionSizer positionSizer =
                positionSizerResolver.resolve(state.getRiskModelType());

        return positionSizer.calculatePositionSize(state);
    }

    /**
     * Resolves final stop price.
     *
     * <p>Priority:
     * 1. explicit stop price from strategy
     * 2. planned stop price from account state
     * 3. no stop -> 0.0</p>
     *
     * @param state current account state
     * @param stopPrice explicit stop price
     * @param entryPrice current entry price
     * @return resolved stop price
     */
    private double resolveStopPrice(
            PaperTradingAccountState state,
            double stopPrice,
            double entryPrice
    ) {
        if (stopPrice > 0.0) {
            return stopPrice;
        }

        if (state.getPlannedStopPrice() != null && state.getPlannedStopPrice() > 0.0) {
            return state.getPlannedStopPrice();
        }

        return 0.0;
    }

    /**
     * Updates planned prices in account state before sizing.
     *
     * <p>This is important for STOP_RISK_PERCENT sizing because the position sizer
     * reads planned entry and planned stop from account state.</p>
     *
     * @param state current account state
     * @param entryPrice resolved entry price
     * @param stopPrice resolved stop price
     */
    private void updatePlannedPricesForSizing(
            PaperTradingAccountState state,
            double entryPrice,
            double stopPrice
    ) {
        state.setPlannedEntryPrice(entryPrice);

        if (stopPrice > 0.0) {
            state.setPlannedStopPrice(stopPrice);
        }
    }

    /**
     * Resolves final target price.
     *
     * <p>If the caller already supplied a target price greater than zero,
     * that explicit target is used.
     * Otherwise the target is calculated from the configured exit model.</p>
     *
     * @param state current account state
     * @param entryPrice entry price
     * @param stopPrice stop price
     * @param targetPrice explicit target price hint
     * @param side position side
     * @return resolved target price
     */
    private double resolveTargetPrice(
            PaperTradingAccountState state,
            double entryPrice,
            double stopPrice,
            double targetPrice,
            PaperPositionSide side
    ) {
        if (targetPrice > 0.0) {
            return targetPrice;
        }

        if (stopPrice <= 0.0) {
            return 0.0;
        }

        return tradePlanService.resolveTargetPrice(
                state,
                entryPrice,
                stopPrice,
                side
        );
    }

    /**
     * Calculates gross PnL for LONG or SHORT positions.
     *
     * @param position open position
     * @param exitPrice exit price
     * @return gross pnl before fees
     */
    private double calculatePnl(PaperTradingPositionDto position, double exitPrice) {
        if (position.getSide() == PaperPositionSide.LONG) {
            return (exitPrice - position.getEntryPrice()) * position.getQuantity();
        }

        return (position.getEntryPrice() - exitPrice) * position.getQuantity();
    }

    /**
     * Calculates fee based on position size and fee percent.
     *
     * @param positionSize trade notional size
     * @param feePercent fee percent
     * @return calculated fee
     */
    private double calculateFee(double positionSize, double feePercent) {
        return positionSize * (feePercent / 100.0);
    }
}