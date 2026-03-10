package com.example.tradingplatform.controller;

import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.CandleService;
import com.example.tradingplatform.service.XChangeCandleService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class CandleRestController {

//    private final CandleService candleService;
    private final XChangeCandleService xChangeCandleService;

    @GetMapping("/api/candles")
    public List<Candle> getCandles(@RequestParam(defaultValue = "BTCUSDT") String ticker) {
        return xChangeCandleService.getCandles(ticker);
    }
}
