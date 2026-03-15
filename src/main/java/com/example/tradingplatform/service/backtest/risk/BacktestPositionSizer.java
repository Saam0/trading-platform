package com.example.tradingplatform.service.backtest.risk;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.model.BacktestRiskModelType;

public interface BacktestPositionSizer {

    BacktestRiskModelType getType();

    double calculatePositionSize(
            BacktestRequest request,
            double capital,
            double entryPrice
    );
}