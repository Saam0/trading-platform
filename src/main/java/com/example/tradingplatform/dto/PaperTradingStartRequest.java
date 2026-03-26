package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.ChartInterval;
import com.example.tradingplatform.model.PaperTradingRiskModelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for starting a new paper trading session.
 */
@Data
public class PaperTradingStartRequest {

    /** Trading symbol, for example BTCUSDT */
    @NotBlank(message = "Ticker is required")
    @Pattern(
            regexp = "^[A-Z0-9]{3,15}$",
            message = "Ticker must contain only uppercase letters and numbers"
    )
    private String ticker = "BTCUSDT";

    /** Trading interval used by the session */
    private ChartInterval interval = ChartInterval.h1;

    /** Strategy code resolved by TradingStrategyResolver */
    @NotBlank(message = "Strategy code is required")
    private String strategyCode;

    /** Initial fake account balance */
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Initial balance must be greater than 0"
    )
    private double initialBalance = 1000.0;

    /** Trading fee percent */
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Fee percent cannot be negative"
    )
    private double feePercent = 0.1;

    /** Default leverage for paper trading */
    @DecimalMin(
            value = "1.0",
            inclusive = true,
            message = "Leverage must be at least 1"
    )
    private double leverage = 1.0;

    /** Position sizing model used by the session */
    private PaperTradingRiskModelType riskModelType = PaperTradingRiskModelType.FULL_BALANCE;

    /**
     * Risk percent per trade.
     *
     * <p>This is especially useful for future stop-based sizing models.</p>
     */
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Risk percent cannot be negative"
    )
    private double riskPercent = 1.0;
}