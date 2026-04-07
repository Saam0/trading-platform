package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.ChartInterval;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Query object for ATR indicator requests.
 */
@Data
public class AtrQuery {

    @NotBlank(message = "Ticker is required")
    @Pattern(
            regexp = "^[A-Z0-9]{3,15}$",
            message = "Ticker must contain only uppercase letters and numbers"
    )
    private String ticker = "BTCUSDT";

    private ChartInterval interval = ChartInterval.d1;

    @Min(value = 1, message = "ATR period must be at least 1")
    private int period = 14;
}