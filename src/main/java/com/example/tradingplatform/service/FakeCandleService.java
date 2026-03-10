package com.example.tradingplatform.service;

import com.example.tradingplatform.model.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FakeCandleService implements CandleService {
    @Override
    public List<Candle> getCandles(String ticker) {
        return switch (ticker.toUpperCase()) {
            case "ETHUSDT" -> getEthCandles();
            case "SOLUSDT" -> getSolCandles();
            case "BTCUSDT" -> getBtcCandles();
            default -> getBtcCandles();
        };
    }

    private List<Candle> getBtcCandles() {
        return List.of(
                new Candle("2026-03-01", 84200, 85150, 83850, 84820, 1250.5),
                new Candle("2026-03-02", 84820, 85600, 84400, 85310, 1480.2),
                new Candle("2026-03-03", 85310, 86040, 85010, 85790, 1325.8),
                new Candle("2026-03-04", 85790, 86120, 84550, 84980, 1660.4),
                new Candle("2026-03-05", 84980, 85430, 84220, 84590, 1195.0),
                new Candle("2026-03-06", 84590, 85210, 84000, 85050, 1412.7),
                new Candle("2026-03-07", 85050, 86400, 84880, 86120, 1710.9),
                new Candle("2026-03-08", 86120, 86890, 85610, 86670, 1804.3),
                new Candle("2026-03-09", 86670, 87250, 86020, 86340, 1544.1),
                new Candle("2026-03-10", 86340, 87010, 85830, 86880, 1698.6)
        );
    }

    private List<Candle> getEthCandles() {
        return List.of(
                new Candle("2026-03-01", 2280, 2315, 2260, 2302, 9200.5),
                new Candle("2026-03-02", 2302, 2340, 2290, 2331, 10480.2),
                new Candle("2026-03-03", 2331, 2362, 2310, 2356, 10025.8),
                new Candle("2026-03-04", 2356, 2368, 2295, 2312, 11660.4),
                new Candle("2026-03-05", 2312, 2328, 2274, 2289, 9195.0),
                new Candle("2026-03-06", 2289, 2322, 2270, 2310, 10412.7),
                new Candle("2026-03-07", 2310, 2380, 2305, 2365, 12710.9),
                new Candle("2026-03-08", 2365, 2402, 2348, 2390, 13804.3),
                new Candle("2026-03-09", 2390, 2410, 2352, 2371, 11544.1),
                new Candle("2026-03-10", 2371, 2420, 2360, 2408, 12698.6)
        );
    }

    private List<Candle> getSolCandles() {
        return List.of(
                new Candle("2026-03-01", 142, 146, 140, 145, 40250.5),
                new Candle("2026-03-02", 145, 148, 143, 147, 42480.2),
                new Candle("2026-03-03", 147, 151, 146, 150, 39025.8),
                new Candle("2026-03-04", 150, 152, 144, 146, 43660.4),
                new Candle("2026-03-05", 146, 147, 141, 143, 37195.0),
                new Candle("2026-03-06", 143, 146, 142, 145, 40412.7),
                new Candle("2026-03-07", 145, 153, 144, 151, 48710.9),
                new Candle("2026-03-08", 151, 156, 149, 154, 51804.3),
                new Candle("2026-03-09", 154, 157, 150, 152, 45544.1),
                new Candle("2026-03-10", 152, 158, 151, 157, 49698.6)
        );
    }
}
