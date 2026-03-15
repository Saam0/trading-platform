package com.example.tradingplatform.service.backtest;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.model.BacktestStrategyType;

public interface BacktestStrategy {

    BacktestStrategyType getType();

    BacktestResultDto run(BacktestRequest request);
}