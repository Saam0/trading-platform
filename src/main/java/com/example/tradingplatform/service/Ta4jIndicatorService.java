package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.ChandelierExitPoint;
import com.example.tradingplatform.dto.SmaPoint;
import com.example.tradingplatform.model.Candle;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class Ta4jIndicatorService implements  IndicatorService {

    private final CandleService candleService;

    public Ta4jIndicatorService(CandleService candleService) {
        this.candleService = candleService;
    }

    @Override
    public List<SmaPoint> getSma(String ticker, String interval, int period) {
        List<Candle> candles = candleService.getCandles(ticker, interval);
        BarSeries series = buildSeries(candles, interval);

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        SMAIndicator smaIndicator = new SMAIndicator(closePriceIndicator, period);

        List<SmaPoint> points = new ArrayList<>();

        for (int i = 0; i < series.getBarCount(); i++) {
            if (i < period - 1) {
                continue;
            }

            Object time = candles.get(i).getTime();
            double value = smaIndicator.getValue(i).doubleValue();

            points.add(new SmaPoint(time, value));
        }

        return points;
    }

    @Override
    public List<SmaPoint> getEma(String ticker, String interval, int period) {
        List<Candle> candles = candleService.getCandles(ticker, interval);
        BarSeries series = buildSeries(candles, interval);

        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        EMAIndicator emaIndicator = new EMAIndicator(closePriceIndicator, period);

        List<SmaPoint> points = new ArrayList<>();

        for (int i = 0; i < series.getBarCount(); i++) {
            if (i < period - 1) {
                continue;
            }

            Object time = candles.get(i).getTime();
            double value = emaIndicator.getValue(i).doubleValue();

            points.add(new SmaPoint(time, value));
        }

        return points;
    }




    @Override
    public List<ChandelierExitPoint> getChandelierExit(
            String ticker,
            String interval,
            int length,
            double multiplier,
            boolean useClose
    ) {
        List<Candle> candles = candleService.getCandles(ticker, interval);

        List<Double> atrValues = calculateAtr(candles, length);
        List<ChandelierExitPoint> points = new ArrayList<>();

        Double previousLongStop = null;
        Double previousShortStop = null;
        int previousDirection = 1;

        for (int i = 0; i < candles.size(); i++) {
            Candle current = candles.get(i);

            if (i < length - 1 || atrValues.get(i) == null) {
                points.add(new ChandelierExitPoint(
                        current.getTime(),
                        null,
                        null,
                        previousDirection,
                        false,
                        false
                ));
                continue;
            }

            double highestValue = useClose
                    ? highestClose(candles, i, length)
                    : highestHigh(candles, i, length);

            double lowestValue = useClose
                    ? lowestClose(candles, i, length)
                    : lowestLow(candles, i, length);

            double atr = atrValues.get(i) * multiplier;

            double longStop = highestValue - atr;
            double shortStop = lowestValue + atr;

            if (previousLongStop != null && i > 0) {
                double previousClose = candles.get(i - 1).getClose();
                if (previousClose > previousLongStop) {
                    longStop = Math.max(longStop, previousLongStop);
                }
            }

            if (previousShortStop != null && i > 0) {
                double previousClose = candles.get(i - 1).getClose();
                if (previousClose < previousShortStop) {
                    shortStop = Math.min(shortStop, previousShortStop);
                }
            }

            int direction = previousDirection;

            if (previousShortStop != null && current.getClose() > previousShortStop) {
                direction = 1;
            } else if (previousLongStop != null && current.getClose() < previousLongStop) {
                direction = -1;
            }

            boolean buySignal = direction == 1 && previousDirection == -1;
            boolean sellSignal = direction == -1 && previousDirection == 1;

            Double plottedLongStop = direction == 1 ? longStop : null;
            Double plottedShortStop = direction == -1 ? shortStop : null;

            points.add(new ChandelierExitPoint(
                    current.getTime(),
                    plottedLongStop,
                    plottedShortStop,
                    direction,
                    buySignal,
                    sellSignal
            ));

            previousLongStop = longStop;
            previousShortStop = shortStop;
            previousDirection = direction;
        }

        return points;
    }

    private List<Double> calculateAtr(List<Candle> candles, int length) {
        List<Double> trueRanges = new ArrayList<>();
        List<Double> atrValues = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            Candle current = candles.get(i);

            double highLow = current.getHigh() - current.getLow();
            double highPrevClose;
            double lowPrevClose;

            if (i == 0) {
                highPrevClose = 0.0;
                lowPrevClose = 0.0;
            } else {
                double previousClose = candles.get(i - 1).getClose();
                highPrevClose = Math.abs(current.getHigh() - previousClose);
                lowPrevClose = Math.abs(current.getLow() - previousClose);
            }

            double trueRange = i == 0
                    ? highLow
                    : Math.max(highLow, Math.max(highPrevClose, lowPrevClose));

            trueRanges.add(trueRange);

            if (i < length - 1) {
                atrValues.add(null);
                continue;
            }

            if (i == length - 1) {
                double sum = 0.0;
                for (int j = 0; j < length; j++) {
                    sum += trueRanges.get(j);
                }
                atrValues.add(sum / length);
                continue;
            }

            double previousAtr = atrValues.get(i - 1);
            double currentAtr = ((previousAtr * (length - 1)) + trueRange) / length;
            atrValues.add(currentAtr);
        }

        return atrValues;
    }

    private double highestHigh(List<Candle> candles, int endIndex, int length) {
        int startIndex = Math.max(0, endIndex - length + 1);
        double highest = Double.NEGATIVE_INFINITY;

        for (int i = startIndex; i <= endIndex; i++) {
            highest = Math.max(highest, candles.get(i).getHigh());
        }

        return highest;
    }

    private double lowestLow(List<Candle> candles, int endIndex, int length) {
        int startIndex = Math.max(0, endIndex - length + 1);
        double lowest = Double.POSITIVE_INFINITY;

        for (int i = startIndex; i <= endIndex; i++) {
            lowest = Math.min(lowest, candles.get(i).getLow());
        }

        return lowest;
    }

    private double highestClose(List<Candle> candles, int endIndex, int length) {
        int startIndex = Math.max(0, endIndex - length + 1);
        double highest = Double.NEGATIVE_INFINITY;

        for (int i = startIndex; i <= endIndex; i++) {
            highest = Math.max(highest, candles.get(i).getClose());
        }

        return highest;
    }

    private double lowestClose(List<Candle> candles, int endIndex, int length) {
        int startIndex = Math.max(0, endIndex - length + 1);
        double lowest = Double.POSITIVE_INFINITY;

        for (int i = startIndex; i <= endIndex; i++) {
            lowest = Math.min(lowest, candles.get(i).getClose());
        }

        return lowest;
    }













    private BarSeries buildSeries(List<Candle> candles, String interval) {
        BarSeries series = new BaseBarSeriesBuilder()
                .withName("price-series")
                .build();

        Duration barDuration = mapDuration(interval);

        for (Candle candle : candles) {
            Instant endTime = toEndTime(candle.getTime()).toInstant();
            series.addBar(toBar(series, candle, barDuration, endTime));
        }

        return series;
    }

    private BaseBar toBar(BarSeries series, Candle candle, Duration barDuration, Instant endTime) {
        Instant beginTime = endTime.minus(barDuration);

        Num open = series.numFactory().numOf(candle.getOpen());
        Num high = series.numFactory().numOf(candle.getHigh());
        Num low = series.numFactory().numOf(candle.getLow());
        Num close = series.numFactory().numOf(candle.getClose());
        Num volume = series.numFactory().numOf(candle.getVolume());
        Num amount = series.numFactory().numOf(candle.getClose() * candle.getVolume());

        return new BaseBar(
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
        );
    }
    private Duration mapDuration(String interval) {
        return switch (interval.toLowerCase()) {
            case "m1" -> Duration.ofMinutes(1);
            case "m5" -> Duration.ofMinutes(5);
            case "m15" -> Duration.ofMinutes(15);
            case "h1" -> Duration.ofHours(1);
            case "h4" -> Duration.ofHours(4);
            case "d1" -> Duration.ofDays(1);
            default -> Duration.ofDays(1);
        };
    }

    private ZonedDateTime toEndTime(Object time) {
        if (time instanceof Number number) {
            long epochSeconds = number.longValue();
            return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC);
        }

        String value = String.valueOf(time);

        if (value.length() == 10) {
            return Instant.parse(value + "T00:00:00Z").atZone(ZoneOffset.UTC);
        }

        return Instant.parse(value + "Z").atZone(ZoneOffset.UTC);
    }
}
