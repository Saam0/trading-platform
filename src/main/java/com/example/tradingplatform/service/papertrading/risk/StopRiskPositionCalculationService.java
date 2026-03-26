package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

/**
 * Calculates stop-based risk sizing values.
 *
 * <p>This service converts:
 * entry price + stop price + risk percent + account balance + fees
 * into position size, quantity and required leverage.</p>
 */
@Service
public class StopRiskPositionCalculationService {

    /**
     * Calculates stop-based sizing values.
     *
     * @param currentBalance current realized account balance
     * @param riskPercent allowed risk percent per trade
     * @param feePercent fee percent per side
     * @param entryPrice planned entry price
     * @param stopPrice planned stop price
     * @return calculated stop-based sizing result
     */
    public StopRiskPositionSizingResult calculate(
            double currentBalance,
            double riskPercent,
            double feePercent,
            double entryPrice,
            double stopPrice
    ) {
        validateInputs(currentBalance, riskPercent, feePercent, entryPrice, stopPrice);

        // calculate how much money is allowed to be lost on this trade
        double riskAmount = currentBalance * (riskPercent / 100.0);

        // calculate absolute stop distance
        double stopDistance = Math.abs(entryPrice - stopPrice);

        // calculate stop distance as percent of entry price
        double stopDistancePercent = (stopDistance * 100.0) / entryPrice;

        // for now assume the same fee on entry and exit
        double entryFeePercent = feePercent;
        double exitFeePercent = feePercent;
        double totalFeePercent = entryFeePercent + exitFeePercent;

        // total effective risk includes price move to stop plus both-side fees
        double effectiveRiskPercent = stopDistancePercent + totalFeePercent;

        if (effectiveRiskPercent <= 0.0) {
            throw new InvalidRequestException("Effective risk percent must be greater than 0");
        }

        // derive notional position size from allowed risk and effective risk percent
        double positionSize = riskAmount / (effectiveRiskPercent / 100.0);

        // derive quantity from notional size and entry price
        double quantity = positionSize / entryPrice;

        // calculate the minimum leverage required to support this notional size
        double requiredLeverage = Math.max(1.0, positionSize / currentBalance);

        return new StopRiskPositionSizingResult(
                riskAmount,
                stopDistance,
                stopDistancePercent,
                entryFeePercent,
                exitFeePercent,
                totalFeePercent,
                effectiveRiskPercent,
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
     * @param feePercent fee percent per side
     * @param entryPrice planned entry price
     * @param stopPrice planned stop price
     */
    private void validateInputs(
            double currentBalance,
            double riskPercent,
            double feePercent,
            double entryPrice,
            double stopPrice
    ) {
        if (currentBalance <= 0.0) {
            throw new InvalidRequestException("Current balance must be greater than 0");
        }

        if (riskPercent <= 0.0) {
            throw new InvalidRequestException("Risk percent must be greater than 0");
        }

        if (feePercent < 0.0) {
            throw new InvalidRequestException("Fee percent cannot be negative");
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