package com.example.tradingplatform.service.papertrading.market;

import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.model.ChartInterval;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a market candle event delivered by a market data feed.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketCandleEvent {

    /** Type of market event */
    private MarketEventType eventType;

    /** Trading symbol, for example BTCUSDT */
    private String ticker;

    /** Candle interval */
    private ChartInterval interval;

    /** Candle associated with the event */
    private Candle candle;
}