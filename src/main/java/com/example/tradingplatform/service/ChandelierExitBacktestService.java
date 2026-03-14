package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.dto.BacktestTradeDto;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.Candle;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChandelierExitBacktestService implements BacktestService {

    private final CandleService candleService;

    public ChandelierExitBacktestService(CandleService candleService) {
        this.candleService = candleService;
    }

    @Override
    public BacktestResultDto runChandelierExitBacktest(BacktestRequest request) {
        List<Candle> candles = candleService.getCandles(
                request.getTicker(),
                request.getInterval(),
                request.getLimit(),
                request.getFrom(),
                request.getTo()
        );

        if (candles.size() < request.getLength() + 1) {
            throw new InvalidRequestException(
                    "Not enough candles for backtest. Need at least "
                            + (request.getLength() + 1)
                            + " candles, but got "
                            + candles.size()
            );
        }

        BarSeries series = buildSeries(candles, request.getInterval());

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        ATRIndicator atrIndicator = new ATRIndicator(series, request.getLength());

        Indicator<Num> upperSource = request.isUseClose()
                ? new ClosePriceIndicator(series)
                : new HighPriceIndicator(series);

        Indicator<Num> lowerSource = request.isUseClose()
                ? new ClosePriceIndicator(series)
                : new LowPriceIndicator(series);

        HighestValueIndicator highestIndicator = new HighestValueIndicator(upperSource, request.getLength());
        LowestValueIndicator lowestIndicator = new LowestValueIndicator(lowerSource, request.getLength());

        List<BacktestTradeDto> trades = new ArrayList<>();

        Num multiplierNum = series.numFactory().numOf(request.getMultiplier());

        Num previousLongStop = null;
        Num previousShortStop = null;
        int previousDirection = 0;

        boolean inPosition = false;
        int entryIndex = -1;
        Object entryTime = null;
        double entryPrice = 0.0;

        double totalPnl = 0.0;
        double equity = 1.0;

        for (int i = request.getLength() - 1; i < series.getBarCount(); i++) {
            Num rawLongStop = highestIndicator.getValue(i)
                    .minus(atrIndicator.getValue(i).multipliedBy(multiplierNum));

            Num rawShortStop = lowestIndicator.getValue(i)
                    .plus(atrIndicator.getValue(i).multipliedBy(multiplierNum));

            Num longStop = rawLongStop;
            Num shortStop = rawShortStop;

            double currentClose = closePriceIndicator.getValue(i).doubleValue();
            int currentDirection;

            if (previousLongStop == null || previousShortStop == null || previousDirection == 0) {
                currentDirection = currentClose >= longStop.doubleValue() ? 1 : -1;
            } else {
                double previousClose = closePriceIndicator.getValue(i - 1).doubleValue();

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

            if (buySignal && !inPosition) {
                inPosition = true;
                entryIndex = i;
                entryTime = candles.get(i).getTime();
                entryPrice = currentClose;
            }

            if (sellSignal && inPosition) {
                BacktestTradeDto trade = closeTrade(
                        candles,
                        entryIndex,
                        entryTime,
                        entryPrice,
                        i,
                        currentClose,
                        "SELL_SIGNAL"
                );

                trades.add(trade);
                totalPnl += trade.getPnl();
                equity *= (1.0 + trade.getPnlPercent() / 100.0);

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
            double lastClose = candles.get(lastIndex).getClose();

            BacktestTradeDto trade = closeTrade(
                    candles,
                    entryIndex,
                    entryTime,
                    entryPrice,
                    lastIndex,
                    lastClose,
                    "FORCED_LAST_CANDLE_EXIT"
            );

            trades.add(trade);
            totalPnl += trade.getPnl();
            equity *= (1.0 + trade.getPnlPercent() / 100.0);
        }

        int winningTrades = 0;
        int losingTrades = 0;

        for (BacktestTradeDto trade : trades) {
            if (trade.getPnl() > 0) {
                winningTrades++;
            } else if (trade.getPnl() < 0) {
                losingTrades++;
            }
        }

        int totalTrades = trades.size();
        double winRate = totalTrades == 0 ? 0.0 : (winningTrades * 100.0) / totalTrades;
        double totalPnlPercent = (equity - 1.0) * 100.0;

        return new BacktestResultDto(
                "Chandelier Exit",
                request.getTicker(),
                request.getInterval(),
                request.getLimit(),
                request.getFrom(),
                request.getTo(),
                request.getLength(),
                request.getMultiplier(),
                request.isUseClose(),
                candles.get(0).getTime(),
                candles.get(candles.size() - 1).getTime(),
                totalTrades,
                winningTrades,
                losingTrades,
                round(winRate),
                round(totalPnl),
                round(totalPnlPercent),
                trades
        );
    }

    private BacktestTradeDto closeTrade(
            List<Candle> candles,
            int entryIndex,
            Object entryTime,
            double entryPrice,
            int exitIndex,
            double exitPrice,
            String exitReason
    ) {
        double pnl = exitPrice - entryPrice;
        double pnlPercent = entryPrice == 0.0 ? 0.0 : (pnl / entryPrice) * 100.0;
        int barsHeld = Math.max(0, exitIndex - entryIndex);

        String result;
        if (pnl > 0) {
            result = "WIN";
        } else if (pnl < 0) {
            result = "LOSS";
        } else {
            result = "BREAKEVEN";
        }

        return new BacktestTradeDto(
                entryTime,
                round(entryPrice),
                candles.get(exitIndex).getTime(),
                round(exitPrice),
                round(pnl),
                round(pnlPercent),
                barsHeld,
                result,
                exitReason
        );
    }

    private BarSeries buildSeries(List<Candle> candles, String interval) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withName("backtest-series")
                .build();

        Duration barDuration = mapDuration(interval);

        for (Candle candle : candles) {
            Instant endTime = toEndTime(candle.getTime()).toInstant();
            Instant beginTime = endTime.minus(barDuration);

            Num open = series.numFactory().numOf(candle.getOpen());
            Num high = series.numFactory().numOf(candle.getHigh());
            Num low = series.numFactory().numOf(candle.getLow());
            Num close = series.numFactory().numOf(candle.getClose());
            Num volume = series.numFactory().numOf(candle.getVolume());
            Num amount = series.numFactory().numOf(candle.getClose() * candle.getVolume());

            series.addBar(new BaseBar(
                    barDuration,
                    beginTime,
                    endTime,
                    open,
                    high,
                    low,
                    close,
                    volume,
                    amount,
                    0L
            ));
        }

        return series;
    }

    private Duration mapDuration(String interval) {
        return switch (interval.toLowerCase()) {
            case "m1" -> Duration.ofMinutes(1);
            case "m5" -> Duration.ofMinutes(5);
            case "m15" -> Duration.ofMinutes(15);
            case "h1" -> Duration.ofHours(1);
            case "h4" -> Duration.ofHours(4);
            case "d1" -> Duration.ofDays(1);
            default -> throw new InvalidRequestException("Unsupported interval: " + interval);
        };
    }

    private ZonedDateTime toEndTime(Object time) {
        if (time instanceof Number number) {
            long epochSeconds = number.longValue();
            return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC);
        }

        String value = String.valueOf(time);

        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC);
        }

        return Instant.parse(value + "Z").atZone(ZoneOffset.UTC);
    }

    private Num max(Num a, Num b) {
        return a.isGreaterThan(b) ? a : b;
    }

    private Num min(Num a, Num b) {
        return a.isLessThan(b) ? a : b;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}