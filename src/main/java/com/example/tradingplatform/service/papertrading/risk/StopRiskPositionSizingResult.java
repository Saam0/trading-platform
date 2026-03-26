package com.example.tradingplatform.service.papertrading.risk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds calculated values for stop-based risk sizing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StopRiskPositionSizingResult {

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

    /** Total fee percent considered in the calculation */
    private double totalFeePercent;

    /** Effective total risk percent = stop distance percent + total fee percent */
    private double effectiveRiskPercent;

    /** Calculated notional position size */
    private double positionSize;

    /** Calculated quantity at the given entry price */
    private double quantity;

    /** Minimum leverage required to open the calculated position size */
    private double requiredLeverage;
}