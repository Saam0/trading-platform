package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.ChartInterval;
import com.example.tradingplatform.model.PaperTradingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the current paper trading session status.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaperTradingStatusDto {

    /** Current session status */
    private PaperTradingSessionStatus status;

    /** Active ticker */
    private String ticker;

    /** Active interval */
    private ChartInterval interval;

    /** Active strategy code */
    private String strategyCode;

    /** Initial session balance */
    private double initialBalance;

    /** Current account balance */
    private double currentBalance;

    /** Trading fee percent */
    private double feePercent;

    /** Trading leverage */
    private double leverage;

    /** Currently open position, if any */
    private PaperTradingPositionDto openPosition;

    /** Closed trade history */
    private List<PaperTradingTradeDto> closedTrades;

    /** Last session event message */
    private String lastEventMessage;
}