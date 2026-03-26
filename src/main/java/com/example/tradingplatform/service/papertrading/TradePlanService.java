package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.model.ExitModelType;
import com.example.tradingplatform.model.PaperPositionSide;
import org.springframework.stereotype.Service;

/**
 * Builds trade plan including stop loss and take profit.
 */
@Service
public class TradePlanService {

    /**
     * Calculates target price based on exit model.
     */
    public double resolveTargetPrice(
            PaperTradingAccountState state,
            double entryPrice,
            double stopPrice,
            PaperPositionSide side
    ) {
        if (state.getExitModelType() == ExitModelType.SIGNAL_ONLY) {
            return 0.0;
        }

        if (state.getExitModelType() == ExitModelType.FIXED_RR) {
            return calculateRRTarget(
                    entryPrice,
                    stopPrice,
                    state.getRiskRewardRatio(),
                    side
            );
        }

        return 0.0;
    }

    private double calculateRRTarget(
            double entry,
            double stop,
            double rr,
            PaperPositionSide side
    ) {
        double risk = Math.abs(entry - stop);

        if (side == PaperPositionSide.LONG) {
            return entry + (risk * rr);
        }

        return entry - (risk * rr);
    }
}