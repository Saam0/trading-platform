package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingPositionDto;
import com.example.tradingplatform.dto.PaperTradingTradeDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current mutable state of a paper trading account.
 */
@Data
public class PaperTradingAccountState {

    /** Initial account balance at session start */
    private double initialBalance;

    /** Current account balance after realized trades */
    private double currentBalance;

    /** Fee percent used for trade calculations */
    private double feePercent;

    /** Leverage used for position sizing */
    private double leverage;

    /** Currently open position */
    private PaperTradingPositionDto openPosition;

    /** History of closed trades */
    private List<PaperTradingTradeDto> closedTrades = new ArrayList<>();
}