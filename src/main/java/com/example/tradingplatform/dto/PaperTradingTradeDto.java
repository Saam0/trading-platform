package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.PaperPositionSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a closed paper trade.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaperTradingTradeDto {

    /** Trade side */
    private PaperPositionSide side;

    /** Entry price */
    private double entryPrice;

    /** Exit price */
    private double exitPrice;

    /** Executed quantity */
    private double quantity;

    /** Position notional size */
    private double positionSize;

    /** Fee paid for the trade */
    private double fee;

    /** Net profit or loss after fee */
    private double pnl;

    /** Human-readable close reason */
    private String reason;
}