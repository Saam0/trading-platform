package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.ChartInterval;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CandleQuery {
    @NotBlank(message = "Ticker is required")
    @Pattern(
            regexp = "^[A-Z0-9]{3,15}$",
            message = "Ticker must contain only uppercase letters and numbers"
    )
    private String ticker = "BTCUSDT";

    private ChartInterval interval = ChartInterval.d1;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 500, message = "Limit must not be greater than 500")
    private int limit = 30;

    /**
     * Unix time in milliseconds.
     * If null -> load latest candles.
     * If present -> load candles up to this moment.
     */
    private Long to;
}
