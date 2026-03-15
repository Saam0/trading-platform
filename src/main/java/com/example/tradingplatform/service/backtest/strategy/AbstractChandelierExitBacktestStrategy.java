package com.example.tradingplatform.service.backtest.strategy;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.dto.BacktestTradeDto;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.BacktestExitModelType;
import com.example.tradingplatform.model.BacktestStrategyType;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.backtest.BacktestStrategy;
import com.example.tradingplatform.service.backtest.risk.BacktestPositionSizer;
import com.example.tradingplatform.service.backtest.risk.BacktestPositionSizerResolver;
import com.example.tradingplatform.service.candle.CandleService;
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
    protected final BacktestPositionSizerResolver positionSizerResolver;

    protected AbstractChandelierExitBacktestStrategy(
            CandleService candleService,
            BacktestPositionSizerResolver positionSizerResolver
    ) {
        this.candleService = candleService;
        this.positionSizerResolver = positionSizerResolver;
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

    protected boolean isFixedRrTpEnabled(BacktestRequest request) {
        return request.getExitModelType() == BacktestExitModelType.FIXED_RR_TP;
    }

    protected double calculateLongTarget(double entryPrice, double stopPrice, double riskRewardRatio) {
        double riskDistance = entryPrice - stopPrice;
        if (riskDistance <= 0.0) {
            return entryPrice;
        }
        return entryPrice + (riskDistance * riskRewardRatio);
    }

    protected double calculateShortTarget(double entryPrice, double stopPrice, double riskRewardRatio) {
        double riskDistance = stopPrice - entryPrice;
        if (riskDistance <= 0.0) {
            return entryPrice;
        }
        return entryPrice - (riskDistance * riskRewardRatio);
    }

    protected boolean isLongTargetHit(Candle candle, double targetPrice) {
        return candle.getHigh() >= targetPrice;
    }

    protected boolean isShortTargetHit(Candle candle, double targetPrice) {
        return candle.getLow() <= targetPrice;
    }

    protected TradeExecution buildTrade(
            String side,
            Object entryTime,
            double entryPrice,
            double stopPrice,
            double targetPrice,
            Object exitTime,
            double exitPrice,
            int entryIndex,
            int exitIndex,
            String exitReason,
            double capitalBefore,
            BacktestRequest request
    ) {
        BacktestPositionSizer positionSizer = positionSizerResolver.resolve(request.getRiskModelType());

        double rawPositionSize = positionSizer.calculatePositionSize(request, capitalBefore, entryPrice);
        double positionSize = Math.max(0.0, rawPositionSize);
        double quantity = entryPrice == 0.0 ? 0.0 : positionSize / entryPrice;

        double grossPnl = "LONG".equals(side)
                ? (exitPrice - entryPrice) * quantity
                : (entryPrice - exitPrice) * quantity;

        double feeRate = request.getFeePercent() / 100.0;
        double entryFee = positionSize * feeRate;
        double exitFee = positionSize * feeRate;
        double totalFee = entryFee + exitFee;

        double netPnl = grossPnl - totalFee;
        double capitalAfter = capitalBefore + netPnl;

        double pnlPercent = positionSize == 0.0 ? 0.0 : (netPnl / positionSize) * 100.0;
        int barsHeld = Math.max(0, exitIndex - entryIndex);

        String result;
        if (netPnl > 0) {
            result = "WIN";
        } else if (netPnl < 0) {
            result = "LOSS";
        } else {
            result = "BREAKEVEN";
        }

        BacktestTradeDto trade = new BacktestTradeDto(
                side,
                entryTime,
                round(entryPrice),
                round(stopPrice),
                round(targetPrice),
                round(quantity),
                round(positionSize),
                round(totalFee),
                round(capitalBefore),
                round(capitalAfter),
                exitTime,
                round(exitPrice),
                round(netPnl),
                round(pnlPercent),
                barsHeld,
                result,
                exitReason
        );

        return new TradeExecution(trade, capitalAfter);
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
        double finalCapital = request.getInitialCapital();

        for (BacktestTradeDto trade : trades) {
            totalPnl += trade.getPnl();
            finalCapital = trade.getCapitalAfter();

            if (trade.getPnl() > 0) {
                winningTrades++;
            } else if (trade.getPnl() < 0) {
                losingTrades++;
            }
        }

        int totalTrades = trades.size();
        double winRate = totalTrades == 0 ? 0.0 : (winningTrades * 100.0) / totalTrades;
        double netProfit = finalCapital - request.getInitialCapital();
        double netProfitPercent = request.getInitialCapital() == 0.0
                ? 0.0
                : (netProfit / request.getInitialCapital()) * 100.0;
        double totalPnlPercent = netProfitPercent;

        return new BacktestResultDto(
                strategyName,
                strategyType,
                request.getExitModelType(),
                round(request.getRiskRewardRatio()),
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

    protected static class TradeExecution {
        private final BacktestTradeDto trade;
        private final double capitalAfter;

        private TradeExecution(BacktestTradeDto trade, double capitalAfter) {
            this.trade = trade;
            this.capitalAfter = capitalAfter;
        }

        public BacktestTradeDto getTrade() {
            return trade;
        }

        public double getCapitalAfter() {
            return capitalAfter;
        }
    }
}