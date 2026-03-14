package com.example.tradingplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BacktestRequest {
    @NotBlank(message = "Ticker is required")
    @Pattern(
            regexp = "^[A-Z0-9]{3,15}$",
            message = "Ticker must contain only uppercase letters and numbers"
    )
    private String ticker = "BTCUSDT";

    @NotBlank(message = "Interval is required")
    @Pattern(
            regexp = "^(m1|m5|m15|h1|h4|d1)$",
            message = "Interval must be one of: m1, m5, m15, h1, h4, d1"
    )
    private String interval = "d1";

    @Min(value = 20, message = "Limit must be at least 20")
    @Max(value = 1000, message = "Limit must be at most 1000")
    private int limit = 300;

    @Positive(message = "Parameter 'to' must be positive")
    private Long to;

    @Min(value = 1, message = "Length must be at least 1")
    @Max(value = 500, message = "Length must be at most 500")
    private int length = 22;

    @DecimalMin(value = "0.1", message = "Multiplier must be at least 0.1")
    private double multiplier = 3.0;

    private boolean useClose = true;
}
