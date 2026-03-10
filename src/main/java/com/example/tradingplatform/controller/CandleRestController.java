package com.example.tradingplatform.controller;

import com.example.tradingplatform.model.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CandleRestController {

    @GetMapping("/api/candles")
    public List<Candle> getCandles() {
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
}
