package com.example.tradingplatform.service;

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
