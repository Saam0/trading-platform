package com.example.tradingplatform.service.backtest;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;

public interface BacktestService {

    BacktestResultDto runBacktest(BacktestRequest request);

}