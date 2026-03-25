package com.example.tradingplatform.model;

/**
 * Represents a generic trading action produced by a strategy.
 *
 * <p>This enum allows any strategy implementation to communicate
 * its decision to the trading engine in a standardized way.</p>
 */
public enum TradingSignalAction {

    /** Open a long position */
    ENTER_LONG,

    /** Open a short position */
    ENTER_SHORT,

    /** Close the currently open long position */
    EXIT_LONG,

    /** Close the currently open short position */
    EXIT_SHORT,

    /** Do nothing */
    HOLD
}