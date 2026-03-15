package com.example.tradingplatform.service.backtest.risk;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.model.BacktestRiskModelType;
import org.springframework.stereotype.Component;

@Component
public class FixedNotionalPositionSizer implements BacktestPositionSizer {

    @Override
    public BacktestRiskModelType getType() {
        return BacktestRiskModelType.FIXED_NOTIONAL;
    }

    @Override
    public double calculatePositionSize(
            BacktestRequest request,
            double capital,
            double entryPrice
    ) {
        double leverage = request.getLeverage() <= 0 ? 1.0 : request.getLeverage();
        double maxAllowedNotional = capital * leverage;

        double requestedNotional = request.getFixedNotional() <= 0.0
                ? maxAllowedNotional
                : request.getFixedNotional();

        return Math.min(requestedNotional, maxAllowedNotional);
    }
}