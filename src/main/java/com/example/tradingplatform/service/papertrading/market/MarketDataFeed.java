package com.example.tradingplatform.service.papertrading.market;

import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.model.ChartInterval;

import java.util.List;

/**
 * Generic market data feed contract for paper/live trading engines.
 *
 * <p>This abstraction allows the engine to work with different data sources
 * such as REST polling, WebSocket streaming, or historical replay.</p>
 */
public interface MarketDataFeed {

    /**
     * Returns the latest fully closed candle event for the given market.
     *
     * @param ticker trading symbol
     * @param interval candle interval
     * @return latest closed candle event
     */
    MarketCandleEvent pollLatestClosedCandle(String ticker, ChartInterval interval);

    /**
     * Returns recent candles for strategy evaluation.
     *
     * @param ticker trading symbol
     * @param interval candle interval
     * @param limit number of candles to load
     * @return recent candles in chronological order
     */
    List<Candle> getRecentCandles(String ticker, ChartInterval interval, int limit);
}