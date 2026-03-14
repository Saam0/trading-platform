package com.example.tradingplatform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BacktestRequest {
    @NotBlank
    private String ticker = "BTCUSDT";

    @NotBlank
    private String interval = "d1";

    @Min(50)
    @Max(2000)
    private int limit = 300;

    private Long to;

    @Min(1)
    private int length = 22;

    @DecimalMin("0.1")
    private double multiplier = 3.0;

    private boolean useClose = true;
}
