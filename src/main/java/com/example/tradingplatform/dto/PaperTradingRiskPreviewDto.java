package com.example.tradingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for paper trading stop-based risk preview.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaperTradingRiskPreviewDto {

    /** Maximum amount allowed to lose on the trade */
    private double riskAmount;

    /** Absolute distance between entry and stop */
    private double stopDistance;

    /** Stop distance expressed as percent of entry price */
    private double stopDistancePercent;

    /** Entry fee percent */
    private double entryFeePercent;

    /** Exit fee percent */
    private double exitFeePercent;

    /** Total fee percent */
    private double totalFeePercent;

    /** Effective risk percent including fees */
    private double effectiveRiskPercent;

    /** Calculated notional position size */
    private double positionSize;

    /** Calculated quantity */
    private double quantity;

    /** Minimum required leverage */
    private double requiredLeverage;
}