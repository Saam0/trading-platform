package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.model.ChartInterval;
import com.example.tradingplatform.model.PaperTradingSessionStatus;
import lombok.Data;

/**
 * Holds the current paper trading session metadata and account state.
 */
@Data
public class PaperTradingSession {

    /** Current session status */
    private PaperTradingSessionStatus status = PaperTradingSessionStatus.STOPPED;

    /** Active ticker */
    private String ticker;

    /** Active interval */
    private ChartInterval interval;

    /** Active strategy code */
    private String strategyCode;

    /** Last session-level message */
    private String lastEventMessage;

    /** Account state attached to the session */
    private PaperTradingAccountState accountState;

    /** Unique key of the last processed candle to avoid duplicate processing */
    private String lastProcessedCandleKey;
}