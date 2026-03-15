package com.example.tradingplatform.service.backtest.strategy;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.dto.BacktestTradeDto;
import com.example.tradingplatform.model.BacktestStrategyType;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.backtest.risk.BacktestPositionSizerResolver;
import com.example.tradingplatform.service.candle.CandleService;
import org.springframework.stereotype.Service;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChandelierExitReversalRetraceBacktestStrategy extends AbstractChandelierExitBacktestStrategy {

    private static final String SIDE_LONG = "LONG";
    private static final String SIDE_SHORT = "SHORT";

    public ChandelierExitReversalRetraceBacktestStrategy(
            CandleService candleService,
            BacktestPositionSizerResolver positionSizerResolver
    ) {
        super(candleService, positionSizerResolver);
    }

    @Override
    public BacktestStrategyType getType() {
        return BacktestStrategyType.CE_REVERSAL_RETRACE;
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

        OpenPosition openPosition = null;
        PendingEntry pendingEntry = null;
        double capital = request.getInitialCapital();

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

            if (openPosition != null) {
                if (SIDE_LONG.equals(openPosition.side) && sellSignal) {
                    TradeExecution execution = buildTrade(
                            SIDE_LONG,
                            openPosition.entryTime,
                            openPosition.entryPrice,
                            candles.get(i).getTime(),
                            currentClose,
                            openPosition.entryIndex,
                            i,
                            "SELL_SIGNAL",
                            capital,
                            request
                    );

                    trades.add(execution.getTrade());
                    capital = execution.getCapitalAfter();
                    openPosition = null;

                    if (previousLongStop != null) {
                        pendingEntry = new PendingEntry(
                                SIDE_SHORT,
                                previousLongStop.doubleValue(),
                                i
                        );
                    }
                } else if (SIDE_SHORT.equals(openPosition.side) && buySignal) {
                    TradeExecution execution = buildTrade(
                            SIDE_SHORT,
                            openPosition.entryTime,
                            openPosition.entryPrice,
                            candles.get(i).getTime(),
                            currentClose,
                            openPosition.entryIndex,
                            i,
                            "BUY_SIGNAL",
                            capital,
                            request
                    );

                    trades.add(execution.getTrade());
                    capital = execution.getCapitalAfter();
                    openPosition = null;

                    if (previousShortStop != null) {
                        pendingEntry = new PendingEntry(
                                SIDE_LONG,
                                previousShortStop.doubleValue(),
                                i
                        );
                    }
                }
            }

            if (openPosition == null && pendingEntry != null && i > pendingEntry.createdIndex) {
                Candle candle = candles.get(i);

                if (SIDE_SHORT.equals(pendingEntry.side) && candle.getHigh() >= pendingEntry.triggerPrice) {
                    openPosition = new OpenPosition(
                            SIDE_SHORT,
                            candles.get(i).getTime(),
                            i,
                            pendingEntry.triggerPrice
                    );
                    pendingEntry = null;
                } else if (SIDE_LONG.equals(pendingEntry.side) && candle.getLow() <= pendingEntry.triggerPrice) {
                    openPosition = new OpenPosition(
                            SIDE_LONG,
                            candles.get(i).getTime(),
                            i,
                            pendingEntry.triggerPrice
                    );
                    pendingEntry = null;
                }
            }

            if (openPosition == null && pendingEntry == null) {
                if (buySignal && previousShortStop != null) {
                    pendingEntry = new PendingEntry(
                            SIDE_LONG,
                            previousShortStop.doubleValue(),
                            i
                    );
                } else if (sellSignal && previousLongStop != null) {
                    pendingEntry = new PendingEntry(
                            SIDE_SHORT,
                            previousLongStop.doubleValue(),
                            i
                    );
                }
            }

            previousLongStop = longStop;
            previousShortStop = shortStop;
            previousDirection = currentDirection;
        }

        if (openPosition != null) {
            int lastIndex = candles.size() - 1;

            TradeExecution execution = buildTrade(
                    openPosition.side,
                    openPosition.entryTime,
                    openPosition.entryPrice,
                    candles.get(lastIndex).getTime(),
                    candles.get(lastIndex).getClose(),
                    openPosition.entryIndex,
                    lastIndex,
                    "FORCED_LAST_CANDLE_EXIT",
                    capital,
                    request
            );

            trades.add(execution.getTrade());
        }

        return buildResult(
                "Chandelier Exit Reversal Retrace",
                BacktestStrategyType.CE_REVERSAL_RETRACE,
                request,
                candles,
                trades
        );
    }

    private static class PendingEntry {
        private final String side;
        private final double triggerPrice;
        private final int createdIndex;

        private PendingEntry(String side, double triggerPrice, int createdIndex) {
            this.side = side;
            this.triggerPrice = triggerPrice;
            this.createdIndex = createdIndex;
        }
    }

    private static class OpenPosition {
        private final String side;
        private final Object entryTime;
        private final int entryIndex;
        private final double entryPrice;

        private OpenPosition(String side, Object entryTime, int entryIndex, double entryPrice) {
            this.side = side;
            this.entryTime = entryTime;
            this.entryIndex = entryIndex;
            this.entryPrice = entryPrice;
        }
    }
}