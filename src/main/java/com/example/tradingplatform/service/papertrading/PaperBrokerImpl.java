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

    public PaperBrokerImpl(PaperTradingPositionSizerResolver positionSizerResolver) {
        this.positionSizerResolver = positionSizerResolver;
    }

    /**
     * Opens a long position using the configured position sizing model.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price
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

        // calculate position size using the configured sizing model
        double positionSize = calculatePositionSize(state);

        // derive quantity from entry price
        double quantity = positionSize / price;

        PaperTradingPositionDto position = new PaperTradingPositionDto(
                PaperPositionSide.LONG,
                price,
                quantity,
                positionSize,
                stopPrice,
                targetPrice
        );

        // store the newly opened position in account state
        state.setOpenPosition(position);

        return position;
    }

    /**
     * Opens a short position using the configured position sizing model.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price
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

        // calculate position size using the configured sizing model
        double positionSize = calculatePositionSize(state);

        // derive quantity from entry price
        double quantity = positionSize / price;

        PaperTradingPositionDto position = new PaperTradingPositionDto(
                PaperPositionSide.SHORT,
                price,
                quantity,
                positionSize,
                stopPrice,
                targetPrice
        );

        // store the newly opened position in account state
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

        // calculate raw PnL before trading fee
        double grossPnl = calculatePnl(position, price);

        // calculate fee using position notional size
        double fee = calculateFee(position.getPositionSize(), state.getFeePercent());

        // final realized PnL after fee
        double netPnl = grossPnl - fee;

        // update realized account balance
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

        // append trade history
        state.getClosedTrades().add(trade);

        // clear open position after closing
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