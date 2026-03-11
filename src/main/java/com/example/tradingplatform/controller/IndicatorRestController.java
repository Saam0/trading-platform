package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.SmaPoint;
import com.example.tradingplatform.service.IndicatorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class IndicatorRestController {
    private final IndicatorService indicatorService;

    @GetMapping("/api/indicators/sma")
    public List<SmaPoint> getSma(
            @RequestParam(defaultValue = "BTCUSDT") String ticker,
            @RequestParam(defaultValue = "d1") String interval,
            @RequestParam(defaultValue = "5") int period
    ) {
        return indicatorService.getSma(ticker, interval, period);
    }

}
