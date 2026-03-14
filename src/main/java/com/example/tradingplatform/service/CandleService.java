package com.example.tradingplatform.service;

import com.example.tradingplatform.model.Candle;

import java.util.List;

public interface CandleService {

    List<Candle> getCandles(String ticker, String interval, int limit, Long from, Long to);

    default List<Candle> getCandles(String ticker, String interval, int limit, Long to) {
        return getCandles(ticker, interval, limit, null, to);
    }
}