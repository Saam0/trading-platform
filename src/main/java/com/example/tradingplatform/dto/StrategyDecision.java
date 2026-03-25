package com.example.tradingplatform.dto;

import com.example.tradingplatform.model.TradingSignalAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a strategy output decision.
 *
 * <p>The trading engine reads this object and decides how to act
 * on the broker/account state.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyDecision {

    /** Action suggested by the strategy */
    private TradingSignalAction action;

    /** Human-readable reason for the decision */
    private String reason;

    /** Optional suggested stop price */
    private Double suggestedStopPrice;

    /** Optional suggested target price */
    private Double suggestedTargetPrice;

    /**
     * Creates a HOLD decision with a reason.
     *
     * @param reason explanation why no action should be taken
     * @return hold decision
     */
    public static StrategyDecision hold(String reason) {
        return new StrategyDecision(TradingSignalAction.HOLD, reason, null, null);
    }
}