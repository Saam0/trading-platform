package com.example.tradingplatform.service.backtest.strategy;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.dto.BacktestTradeDto;
import com.example.tradingplatform.model.BacktestStrategyType;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.candle.CandleService;
import org.springframework.stereotype.Service;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChandelierExitShortOnlyBacktestStrategy extends AbstractChandelierExitBacktestStrategy {

    public ChandelierExitShortOnlyBacktestStrategy(CandleService candleService) {
        super(candleService);
    }

    @Override
    public BacktestStrategyType getType() {
        return BacktestStrategyType.CE_SHORT_ONLY;
    }

    @Override
    public BacktestResultDto run(BacktestRequest request) {
        validateRequest(request);

        List<Candle> candles = loadCandles(request);
        CeContext context = buildCeContext(candles, request);

        List<BacktestTradeDto> trades = new ArrayList<>();
        Num multiplierNum = context.getSeries().numFactory().numOf(request.getMultiplier());

        Num previousLongStop = null;
        Num previousShortStop = null;
        int previousDirection = 0;

        boolean inPosition = false;
        int entryIndex = -1;
        Object entryTime = null;
        double entryPrice = 0.0;

        for (int i = request.getLength() - 1; i < context.getSeries().getBarCount(); i++) {
            Num rawLongStop = context.getHighestIndicator().getValue(i)
                    .minus(context.getAtrIndicator().getValue(i).multipliedBy(multiplierNum));

            Num rawShortStop = context.getLowestIndicator().getValue(i)
                    .plus(context.getAtrIndicator().getValue(i).multipliedBy(multiplierNum));

            Num longStop = rawLongStop;
            Num shortStop = rawShortStop;

            double currentClose = context.getClosePriceIndicator().getValue(i).doubleValue();
            int currentDirection;

            if (previousLongStop == null || previousShortStop == null || previousDirection == 0) {
                currentDirection = currentClose >= longStop.doubleValue() ? 1 : -1;
            } else {
                double previousClose = context.getClosePriceIndicator().getValue(i - 1).doubleValue();

                if (previousClose > previousLongStop.doubleValue()) {
                    longStop = max(rawLongStop, previousLongStop);
                }

                if (previousClose < previousShortStop.doubleValue()) {
                    shortStop = min(rawShortStop, previousShortStop);
                }

                currentDirection = previousDirection;

                if (currentClose > previousShortStop.doubleValue()) {
                    currentDirection = 1;
                } else if (currentClose < previousLongStop.doubleValue()) {
                    currentDirection = -1;
                }
            }

            boolean buySignal = previousDirection == -1 && currentDirection == 1;
            boolean sellSignal = previousDirection == 1 && currentDirection == -1;

            if (sellSignal && !inPosition) {
                inPosition = true;
                entryIndex = i;
                entryTime = candles.get(i).getTime();
                entryPrice = currentClose;
            }

            if (buySignal && inPosition) {
                trades.add(buildTrade(
                        "SHORT",
                        entryTime,
                        entryPrice,
                        candles.get(i).getTime(),
                        currentClose,
                        entryIndex,
                        i,
                        "BUY_SIGNAL"
                ));

                inPosition = false;
                entryIndex = -1;
                entryTime = null;
                entryPrice = 0.0;
            }

            previousLongStop = longStop;
            previousShortStop = shortStop;
            previousDirection = currentDirection;
        }

        if (inPosition) {
            int lastIndex = candles.size() - 1;

            trades.add(buildTrade(
                    "SHORT",
                    entryTime,
                    entryPrice,
                    candles.get(lastIndex).getTime(),
                    candles.get(lastIndex).getClose(),
                    entryIndex,
                    lastIndex,
                    "FORCED_LAST_CANDLE_EXIT"
            ));
        }

        return buildResult(
                "Chandelier Exit Short Only",
                BacktestStrategyType.CE_SHORT_ONLY,
                request,
                candles,
                trades
        );
    }
}