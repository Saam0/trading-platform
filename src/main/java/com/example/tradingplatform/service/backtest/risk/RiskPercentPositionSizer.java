package com.example.tradingplatform.service.backtest.risk;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.model.BacktestRiskModelType;
import org.springframework.stereotype.Component;

@Component
public class RiskPercentPositionSizer implements BacktestPositionSizer {

    @Override
    public BacktestRiskModelType getType() {
        return BacktestRiskModelType.RISK_PERCENT;
    }

    @Override
    public double calculatePositionSize(
            BacktestRequest request,
            double capital,
            double entryPrice
    ) {
        double leverage = request.getLeverage() <= 0 ? 1.0 : request.getLeverage();
        double riskPercent = request.getRiskPercent() <= 0.0 ? 1.0 : request.getRiskPercent();

        double positionSize = capital * (riskPercent / 100.0) * leverage;
        double maxAllowedNotional = capital * leverage;

        return Math.min(positionSize, maxAllowedNotional);
    }
}