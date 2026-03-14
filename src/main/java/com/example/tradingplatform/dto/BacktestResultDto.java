package com.example.tradingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BacktestResultDto {

    private String strategy;

    private String ticker;

    private String interval;

    private int limit;

    private int totalTrades;

    private int winningTrades;

    private int losingTrades;

    private double winRate;

    private double totalPnl;

    private double totalPnlPercent;

    private List<BacktestTradeDto> trades;
}
