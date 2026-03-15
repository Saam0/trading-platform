package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.BacktestRequest;
import org.springframework.stereotype.Component;

@Component
public class FullCapitalPositionSizer implements BacktestPositionSizer {

    @Override
    public double calculatePositionSize(
            BacktestRequest request,
            double capital,
            double entryPrice
    ) {

        double leverage = request.getLeverage() <= 0 ? 1 : request.getLeverage();

        return capital * leverage;
    }
}