package com.example.tradingplatform.service.backtest.strategy;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.dto.BacktestTradeDto;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.BacktestStrategyType;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.candle.CandleService;
import com.example.tradingplatform.service.backtest.BacktestStrategy;
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
import java.util.List;

public abstract class AbstractChandelierExitBacktestStrategy implements BacktestStrategy {

    protected final CandleService candleService;

    protected AbstractChandelierExitBacktestStrategy(CandleService candleService) {
        this.candleService = candleService;
    }

    protected void validateRequest(BacktestRequest request) {
        if (request.getFrom() != null && request.getTo() != null && request.getFrom() >= request.getTo()) {
            throw new InvalidRequestException("Parameter 'from' must be less than 'to'");
        }
    }

    protected List<Candle> loadCandles(BacktestRequest request) {
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

        return candles;
    }

    protected BarSeries buildSeries(List<Candle> candles, String interval) {
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

    protected CeContext buildCeContext(List<Candle> candles, BacktestRequest request) {
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

        return new CeContext(series, closePriceIndicator, atrIndicator, highestIndicator, lowestIndicator);
    }

    protected BacktestTradeDto buildTrade(
            String side,
            Object entryTime,
            double entryPrice,
            Object exitTime,
            double exitPrice,
            int entryIndex,
            int exitIndex,
            String exitReason
    ) {
        double pnl = "LONG".equals(side)
                ? exitPrice - entryPrice
                : entryPrice - exitPrice;

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
                side,
                entryTime,
                round(entryPrice),
                exitTime,
                round(exitPrice),
                round(pnl),
                round(pnlPercent),
                barsHeld,
                result,
                exitReason
        );
    }

    protected BacktestResultDto buildResult(
            String strategyName,
            BacktestStrategyType strategyType,
            BacktestRequest request,
            List<Candle> candles,
            List<BacktestTradeDto> trades
    ) {
        int winningTrades = 0;
        int losingTrades = 0;
        double totalPnl = 0.0;

        for (BacktestTradeDto trade : trades) {
            totalPnl += trade.getPnl();

            if (trade.getPnl() > 0) {
                winningTrades++;
            } else if (trade.getPnl() < 0) {
                losingTrades++;
            }
        }

        int totalTrades = trades.size();
        double winRate = totalTrades == 0 ? 0.0 : (winningTrades * 100.0) / totalTrades;
        double finalCapital = request.getInitialCapital() + totalPnl;
        double netProfit = totalPnl;
        double netProfitPercent = request.getInitialCapital() == 0.0
                ? 0.0
                : (netProfit / request.getInitialCapital()) * 100.0;
        double totalPnlPercent = netProfitPercent;

        return new BacktestResultDto(
                strategyName,
                strategyType,
                request.getTicker(),
                request.getInterval(),
                request.getLimit(),
                request.getFrom(),
                request.getTo(),
                request.getLength(),
                request.getMultiplier(),
                request.isUseClose(),
                round(request.getInitialCapital()),
                round(finalCapital),
                round(netProfit),
                round(netProfitPercent),
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

    protected Duration mapDuration(String interval) {
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

    protected ZonedDateTime toEndTime(Object time) {
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

    protected Num max(Num a, Num b) {
        return a.isGreaterThan(b) ? a : b;
    }

    protected Num min(Num a, Num b) {
        return a.isLessThan(b) ? a : b;
    }

    protected double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    protected static class CeContext {
        private final BarSeries series;
        private final ClosePriceIndicator closePriceIndicator;
        private final ATRIndicator atrIndicator;
        private final HighestValueIndicator highestIndicator;
        private final LowestValueIndicator lowestIndicator;

        private CeContext(
                BarSeries series,
                ClosePriceIndicator closePriceIndicator,
                ATRIndicator atrIndicator,
                HighestValueIndicator highestIndicator,
                LowestValueIndicator lowestIndicator
        ) {
            this.series = series;
            this.closePriceIndicator = closePriceIndicator;
            this.atrIndicator = atrIndicator;
            this.highestIndicator = highestIndicator;
            this.lowestIndicator = lowestIndicator;
        }

        public BarSeries getSeries() {
            return series;
        }

        public ClosePriceIndicator getClosePriceIndicator() {
            return closePriceIndicator;
        }

        public ATRIndicator getAtrIndicator() {
            return atrIndicator;
        }

        public HighestValueIndicator getHighestIndicator() {
            return highestIndicator;
        }

        public LowestValueIndicator getLowestIndicator() {
            return lowestIndicator;
        }
    }
}