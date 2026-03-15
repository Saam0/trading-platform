package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.BacktestExitModelType;
import com.example.tradingplatform.model.BacktestStrategyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BacktestResultDto {

    private String strategy;

    private BacktestStrategyType strategyType;

    private BacktestExitModelType exitModelType;

    private double riskRewardRatio;

    private String ticker;

    private String interval;

    private int limit;

    private Long from;

    private Long to;

    private int length;

    private double multiplier;

    private boolean useClose;

    private double initialCapital;

    private double finalCapital;

    private double netProfit;

    private double netProfitPercent;

    private Object startTime;

    private Object endTime;

    private int totalTrades;

    private int winningTrades;

    private int losingTrades;

    private double winRate;

    private double totalPnl;

    private double totalPnlPercent;

    private List<BacktestTradeDto> trades;
}