package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.BacktestRequest;

public interface BacktestPositionSizer {

    double calculatePositionSize(
            BacktestRequest request,
            double capital,
            double entryPrice
    );

}