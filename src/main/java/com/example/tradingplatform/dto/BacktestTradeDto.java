package com.example.tradingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BacktestTradeDto {

    private String side;

    private Object entryTime;

    private double entryPrice;

    private double stopPrice;

    private double targetPrice;

    private double quantity;

    private double positionSize;

    private double fee;

    private double capitalBefore;

    private double capitalAfter;

    private Object exitTime;

    private double exitPrice;

    private double pnl;

    private double pnlPercent;

    private int barsHeld;

    private String result;

    private String exitReason;
}