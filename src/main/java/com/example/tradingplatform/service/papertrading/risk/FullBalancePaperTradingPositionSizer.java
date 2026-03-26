package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.model.PaperTradingRiskModelType;
import com.example.tradingplatform.service.papertrading.PaperTradingAccountState;
import org.springframework.stereotype.Service;

/**
 * Position sizer that uses the full available balance with leverage.
 */
@Service
public class FullBalancePaperTradingPositionSizer implements PaperTradingPositionSizer {

    /**
     * Returns the supported risk model.
     *
     * @return FULL_BALANCE
     */
    @Override
    public PaperTradingRiskModelType getSupportedRiskModel() {
        return PaperTradingRiskModelType.FULL_BALANCE;
    }

    /**
     * Calculates position size using full balance and leverage.
     *
     * @param accountState current account state
     * @return calculated notional position size
     */
    @Override
    public double calculatePositionSize(PaperTradingAccountState accountState) {
        // use the full realized balance with leverage
        return accountState.getCurrentBalance() * accountState.getLeverage();
    }
}