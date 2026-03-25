package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingPositionDto;
import com.example.tradingplatform.dto.PaperTradingTradeDto;
import com.example.tradingplatform.model.PaperPositionSide;

/**
 * Broker abstraction for paper/live execution layers.
 *
 * <p>In this phase we implement only the paper broker,
 * but the contract is designed so a real broker can be added later.</p>
 */
public interface PaperBroker {

    /**
     * Opens a long position.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price
     * @param reason open reason
     * @return created position
     */
    PaperTradingPositionDto openLong(
            PaperTradingAccountState state,
            double price,
            double stopPrice,
            double targetPrice,
            String reason
    );

    /**
     * Opens a short position.
     *
     * @param state current account state
     * @param price entry price
     * @param stopPrice optional stop price
     * @param targetPrice optional target price
     * @param reason open reason
     * @return created position
     */
    PaperTradingPositionDto openShort(
            PaperTradingAccountState state,
            double price,
            double stopPrice,
            double targetPrice,
            String reason
    );

    /**
     * Closes the current open position.
     *
     * @param state current account state
     * @param price exit price
     * @param reason close reason
     * @return closed trade
     */
    PaperTradingTradeDto closePosition(
            PaperTradingAccountState state,
            double price,
            String reason
    );

    /**
     * Checks whether an open position exists.
     *
     * @param state current account state
     * @return true if position exists
     */
    boolean hasOpenPosition(PaperTradingAccountState state);

    /**
     * Checks whether the open position is long.
     *
     * @param state current account state
     * @return true if open long exists
     */
    boolean hasLongPosition(PaperTradingAccountState state);

    /**
     * Checks whether the open position is short.
     *
     * @param state current account state
     * @return true if open short exists
     */
    boolean hasShortPosition(PaperTradingAccountState state);

    /**
     * Returns the current open side, if any.
     *
     * @param state current account state
     * @return open side or null
     */
    PaperPositionSide getOpenSide(PaperTradingAccountState state);
}