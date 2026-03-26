package com.example.tradingplatform.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Request body for paper trading stop-based risk preview calculation.
 */
@Data
public class PaperTradingRiskPreviewRequest {

    /** Current account balance used in the preview */
    @DecimalMin(value = "0.0", inclusive = false, message = "Balance must be greater than 0")
    private double balance;

    /** Allowed risk percent per trade */
    @DecimalMin(value = "0.0", inclusive = false, message = "Risk percent must be greater than 0")
    private double riskPercent;

    /** Fee percent per side */
    @DecimalMin(value = "0.0", inclusive = true, message = "Fee percent cannot be negative")
    private double feePercent;

    /** Planned entry price */
    @DecimalMin(value = "0.0", inclusive = false, message = "Entry price must be greater than 0")
    private double entryPrice;

    /** Planned stop price */
    @DecimalMin(value = "0.0", inclusive = false, message = "Stop price must be greater than 0")
    private double stopPrice;
}