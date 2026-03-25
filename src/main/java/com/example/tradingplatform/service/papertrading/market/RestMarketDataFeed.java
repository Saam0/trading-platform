package com.example.tradingplatform.service.papertrading.market;

import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.model.ChartInterval;
import com.example.tradingplatform.service.candle.CandleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * REST-based implementation of MarketDataFeed.
 *
 * <p>This implementation uses CandleService to fetch candles
 * from the exchange via REST API.</p>
 */
@Service
public class RestMarketDataFeed implements MarketDataFeed {

    /** Number of candles to load for strategy evaluation context */
    private static final int DEFAULT_RECENT_CANDLE_LIMIT = 200;

    /** Candle service used to fetch market data */
    private final CandleService candleService;

    public RestMarketDataFeed(CandleService candleService) {
        this.candleService = candleService;
    }

    /**
     * Polls the latest closed candle and wraps it into a market event.
     *
     * @param ticker trading symbol
     * @param interval candle interval
     * @return latest closed candle event
     */
    @Override
    public MarketCandleEvent pollLatestClosedCandle(String ticker, ChartInterval interval) {

        // fetch only a small recent slice, because we need just the latest closed candle
        List<Candle> candles = candleService.getCandles(ticker, interval.name(), 2, (Long) null);

        if (candles == null || candles.isEmpty()) {
            throw new IllegalStateException("No candles returned from CandleService");
        }

        // take the latest candle from the returned chronological list
        Candle lastCandle = candles.get(candles.size() - 1);

        return new MarketCandleEvent(
                MarketEventType.CANDLE_CLOSED,
                ticker,
                interval,
                lastCandle
        );
    }

    /**
     * Returns recent candles for strategy evaluation.
     *
     * @param ticker trading symbol
     * @param interval candle interval
     * @param limit number of candles to load
     * @return candle list in chronological order
     */
    @Override
    public List<Candle> getRecentCandles(String ticker, ChartInterval interval, int limit) {

        int effectiveLimit = limit > 0 ? limit : DEFAULT_RECENT_CANDLE_LIMIT;

        return candleService.getCandles(ticker, interval.name(), effectiveLimit, (Long) null);
    }
}