package com.example.tradingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BacktestTradeDto {
    private Object entryTime;

    private double entryPrice;

    private Object exitTime;

    private double exitPrice;

    private double pnl;

    private double pnlPercent;

    private int barsHeld;

    private String result;

    private String exitReason;
}
