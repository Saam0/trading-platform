package com.example.tradingplatform.service.papertrading.market;

/**
 * Represents the type of market data event delivered to the trading engine.
 */
public enum MarketEventType {

    /** A candle has fully closed and is ready for strategy evaluation */
    CANDLE_CLOSED
}