package com.example.tradingplatform.service;

import com.example.tradingplatform.model.Candle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class FakeCandleService implements CandleService {

    @Override
    public List<Candle> getCandles(String ticker, String interval, int limit, Long from, Long to) {
        List<Candle> source = switch (ticker.toUpperCase()) {
            case "ETHUSDT" -> getEthCandles();
            case "SOLUSDT" -> getSolCandles();
            case "BTCUSDT" -> getBtcCandles();
            default -> getBtcCandles();
        };

        List<Candle> filtered = new ArrayList<>();

        for (Candle candle : source) {
            long candleTimeMillis = toMillis(candle.getTime());

            boolean matchesFrom = from == null || candleTimeMillis >= from;
            boolean matchesTo = to == null || candleTimeMillis <= to;

            if (matchesFrom && matchesTo) {
                filtered.add(candle);
            }
        }

        if (filtered.size() <= limit) {
            return filtered;
        }

        return filtered.subList(filtered.size() - limit, filtered.size());
    }

    private long toMillis(Object time) {
        if (time instanceof Number number) {
            long value = number.longValue();
            return value > 1_000_000_000_000L ? value : value * 1000;
        }

        String value = String.valueOf(time);

        if (value.length() == 10) {
            return LocalDate.parse(value)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
        }

        return Instant.parse(value + "Z").toEpochMilli();
    }

    private List<Candle> getBtcCandles() {
        return List.of(
                new Candle("2026-02-20", 94400, 95200, 93850, 94950, 1180.5),
                new Candle("2026-02-21", 94950, 95800, 94410, 95520, 1274.9),
                new Candle("2026-02-22", 95520, 96100, 94880, 95110, 1325.7),
                new Candle("2026-02-23", 95110, 95740, 94220, 94480, 1492.3),
                new Candle("2026-02-24", 94480, 94850, 93210, 93620, 1681.2),
                new Candle("2026-02-25", 93620, 94110, 92800, 93150, 1540.6),
                new Candle("2026-02-26", 93150, 93880, 92650, 93690, 1434.1),
                new Candle("2026-02-27", 93690, 94620, 93450, 94320, 1370.5),
                new Candle("2026-02-28", 94320, 95180, 94010, 94980, 1422.4),
                new Candle("2026-03-01", 94980, 95640, 94680, 95410, 1350.8),
                new Candle("2026-03-02", 95410, 95920, 94570, 94810, 1615.9),
                new Candle("2026-03-03", 94810, 95110, 93640, 93980, 1732.1),
                new Candle("2026-03-04", 93980, 94430, 92890, 93260, 1814.0),
                new Candle("2026-03-05", 93260, 93800, 92420, 92840, 1693.5),
                new Candle("2026-03-06", 92840, 93550, 92210, 93490, 1505.2),
                new Candle("2026-03-07", 93490, 94600, 93280, 94270, 1467.8),
                new Candle("2026-03-08", 94270, 95240, 93910, 94990, 1510.3),
                new Candle("2026-03-09", 94990, 95850, 94660, 95620, 1488.6),
                new Candle("2026-03-10", 95620, 96420, 95210, 96110, 1554.4),
                new Candle("2026-03-11", 96110, 96800, 95640, 95840, 1479.0),
                new Candle("2026-03-12", 95840, 96300, 94820, 95150, 1668.7),
                new Candle("2026-03-13", 95150, 95760, 94470, 95540, 1582.2)
        );
    }

    private List<Candle> getEthCandles() {
        return List.of(
                new Candle("2026-02-20", 2620, 2650, 2588, 2635, 8200.4),
                new Candle("2026-02-21", 2635, 2682, 2620, 2670, 8450.7),
                new Candle("2026-02-22", 2670, 2691, 2638, 2644, 8311.2),
                new Candle("2026-02-23", 2644, 2660, 2598, 2608, 8740.9),
                new Candle("2026-02-24", 2608, 2622, 2550, 2561, 9012.6),
                new Candle("2026-02-25", 2561, 2595, 2524, 2532, 9155.4),
                new Candle("2026-02-26", 2532, 2578, 2518, 2560, 8891.1),
                new Candle("2026-02-27", 2560, 2602, 2548, 2588, 8730.2),
                new Candle("2026-02-28", 2588, 2624, 2570, 2612, 8615.8),
                new Candle("2026-03-01", 2612, 2640, 2591, 2629, 8544.3),
                new Candle("2026-03-02", 2629, 2651, 2582, 2590, 9180.7),
                new Candle("2026-03-03", 2590, 2608, 2530, 2544, 9624.5),
                new Candle("2026-03-04", 2544, 2570, 2498, 2510, 9951.0),
                new Candle("2026-03-05", 2510, 2542, 2485, 2498, 9722.8),
                new Candle("2026-03-06", 2498, 2538, 2472, 2526, 9485.1),
                new Candle("2026-03-07", 2526, 2575, 2510, 2568, 9236.4),
                new Candle("2026-03-08", 2568, 2612, 2550, 2599, 9055.2),
                new Candle("2026-03-09", 2599, 2630, 2582, 2618, 8940.8),
                new Candle("2026-03-10", 2618, 2655, 2600, 2646, 9068.9),
                new Candle("2026-03-11", 2646, 2660, 2609, 2620, 8988.0),
                new Candle("2026-03-12", 2620, 2634, 2577, 2592, 9277.5),
                new Candle("2026-03-13", 2592, 2628, 2580, 2610, 9112.3)
        );
    }

    private List<Candle> getSolCandles() {
        return List.of(
                new Candle("2026-02-20", 171, 176, 169, 174, 38200.1),
                new Candle("2026-02-21", 174, 178, 173, 177, 39020.4),
                new Candle("2026-02-22", 177, 179, 174, 175, 40111.8),
                new Candle("2026-02-23", 175, 176, 169, 170, 42770.2),
                new Candle("2026-02-24", 170, 171, 164, 166, 43980.9),
                new Candle("2026-02-25", 166, 168, 161, 163, 45212.6),
                new Candle("2026-02-26", 163, 167, 160, 166, 44510.3),
                new Candle("2026-02-27", 166, 170, 164, 169, 43221.4),
                new Candle("2026-02-28", 169, 173, 167, 172, 42155.8),
                new Candle("2026-03-01", 172, 175, 170, 174, 41590.6),
                new Candle("2026-03-02", 174, 176, 169, 170, 43855.1),
                new Candle("2026-03-03", 170, 171, 164, 166, 46330.2),
                new Candle("2026-03-04", 166, 168, 160, 162, 48940.7),
                new Candle("2026-03-05", 162, 164, 157, 159, 47210.4),
                new Candle("2026-03-06", 159, 163, 156, 162, 45544.1),
                new Candle("2026-03-07", 162, 167, 160, 166, 44010.2),
                new Candle("2026-03-08", 166, 170, 164, 169, 43022.7),
                new Candle("2026-03-09", 169, 173, 167, 171, 42100.5),
                new Candle("2026-03-10", 171, 175, 169, 174, 43330.1),
                new Candle("2026-03-11", 174, 176, 170, 172, 44111.3),
                new Candle("2026-03-12", 172, 173, 167, 168, 45200.9),
                new Candle("2026-03-13", 168, 171, 166, 170, 44780.6)
        );
    }
}