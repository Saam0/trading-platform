package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.PaperPositionSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the currently open paper trading position.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaperTradingPositionDto {

    /** Position side */
    private PaperPositionSide side;

    /** Position entry price */
    private double entryPrice;

    /** Position quantity */
    private double quantity;

    /** Notional position size */
    private double positionSize;

    /** Optional stop price */
    private double stopPrice;

    /** Optional target price */
    private double targetPrice;
}