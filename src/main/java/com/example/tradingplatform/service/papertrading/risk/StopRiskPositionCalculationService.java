package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

/**
 * Calculates stop-based risk sizing values.
 *
 * <p>This service is responsible for converting:
 * entry price + stop price + risk percent + account balance
 * into position size, quantity and required leverage.</p>
 */
@Service
public class StopRiskPositionCalculationService {

    /**
     * Calculates stop-based sizing values.
     *
     * @param currentBalance current realized account balance
     * @param riskPercent allowed risk percent per trade
     * @param entryPrice planned entry price
     * @param stopPrice planned stop price
     * @return calculated stop-based sizing result
     */
    public StopRiskPositionSizingResult calculate(
            double currentBalance,
            double riskPercent,
            double entryPrice,
            double stopPrice
    ) {
        validateInputs(currentBalance, riskPercent, entryPrice, stopPrice);

        // calculate how much money is allowed to be lost on this trade
        double riskAmount = currentBalance * (riskPercent / 100.0);

        // calculate absolute stop distance
        double stopDistance = Math.abs(entryPrice - stopPrice);

        // calculate stop distance as percent of entry price
        double stopDistancePercent = (stopDistance * 100.0) / entryPrice;

        if (stopDistancePercent <= 0.0) {
            throw new InvalidRequestException("Stop distance percent must be greater than 0");
        }

        // derive notional position size from allowed risk and stop distance percent
        double positionSize = riskAmount / (stopDistancePercent / 100.0);

        // derive quantity from notional size and entry price
        double quantity = positionSize / entryPrice;

        // calculate the minimum leverage required to support this notional size
        double requiredLeverage = Math.max(1.0, positionSize / currentBalance);

        return new StopRiskPositionSizingResult(
                riskAmount,
                stopDistance,
                stopDistancePercent,
                positionSize,
                quantity,
                requiredLeverage
        );
    }

    /**
     * Validates stop-based sizing inputs.
     *
     * @param currentBalance current account balance
     * @param riskPercent allowed risk percent
     * @param entryPrice planned entry price
     * @param stopPrice planned stop price
     */
    private void validateInputs(
            double currentBalance,
            double riskPercent,
            double entryPrice,
            double stopPrice
    ) {
        if (currentBalance <= 0.0) {
            throw new InvalidRequestException("Current balance must be greater than 0");
        }

        if (riskPercent <= 0.0) {
            throw new InvalidRequestException("Risk percent must be greater than 0");
        }

        if (entryPrice <= 0.0) {
            throw new InvalidRequestException("Entry price must be greater than 0");
        }

        if (stopPrice <= 0.0) {
            throw new InvalidRequestException("Stop price must be greater than 0");
        }

        if (Double.compare(entryPrice, stopPrice) == 0) {
            throw new InvalidRequestException("Entry price and stop price cannot be equal");
        }
    }
}