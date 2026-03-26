package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.PaperTradingRiskModelType;
import com.example.tradingplatform.service.papertrading.PaperTradingAccountState;
import org.springframework.stereotype.Service;

/**
 * Position sizer that uses stop distance, risk percent and fees
 * to calculate the position size.
 */
@Service
public class StopRiskPercentPaperTradingPositionSizer implements PaperTradingPositionSizer {

    /** Service used to calculate stop-based sizing values */
    private final StopRiskPositionCalculationService stopRiskPositionCalculationService;

    public StopRiskPercentPaperTradingPositionSizer(
            StopRiskPositionCalculationService stopRiskPositionCalculationService
    ) {
        this.stopRiskPositionCalculationService = stopRiskPositionCalculationService;
    }

    /**
     * Returns the supported risk model.
     *
     * @return STOP_RISK_PERCENT
     */
    @Override
    public PaperTradingRiskModelType getSupportedRiskModel() {
        return PaperTradingRiskModelType.STOP_RISK_PERCENT;
    }

    /**
     * Calculates position size using:
     * current balance + risk percent + fee percent + planned entry + planned stop.
     *
     * @param accountState current account state
     * @return calculated notional position size
     */
    @Override
    public double calculatePositionSize(PaperTradingAccountState accountState) {
        validatePlannedPrices(accountState);

        StopRiskPositionSizingResult result = stopRiskPositionCalculationService.calculate(
                accountState.getCurrentBalance(),
                accountState.getRiskPercent(),
                accountState.getFeePercent(),
                accountState.getPlannedEntryPrice(),
                accountState.getPlannedStopPrice()
        );

        return result.getPositionSize();
    }

    /**
     * Validates that planned entry and stop prices are present.
     *
     * @param accountState current account state
     */
    private void validatePlannedPrices(PaperTradingAccountState accountState) {
        if (accountState.getPlannedEntryPrice() == null) {
            throw new InvalidRequestException(
                    "Planned entry price is required for STOP_RISK_PERCENT model"
            );
        }

        if (accountState.getPlannedStopPrice() == null) {
            throw new InvalidRequestException(
                    "Planned stop price is required for STOP_RISK_PERCENT model"
            );
        }
    }
}