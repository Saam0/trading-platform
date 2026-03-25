package com.example.tradingplatform.model;

/**
 * Represents the current status of the paper trading session.
 */
public enum PaperTradingSessionStatus {

    /** Session is not running */
    STOPPED,

    /** Session is active and can process trading events */
    RUNNING
}