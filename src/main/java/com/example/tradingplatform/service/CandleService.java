package com.example.tradingplatform.service;

import com.example.tradingplatform.model.Candle;

import java.util.List;

public interface CandleService {
    List<Candle> getCandles(String ticker, String interval);;
}
