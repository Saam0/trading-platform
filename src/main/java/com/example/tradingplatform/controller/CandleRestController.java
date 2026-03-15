package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.CandleQuery;
import com.example.tradingplatform.model.Candle;
import com.example.tradingplatform.service.candle.CandleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@AllArgsConstructor
public class CandleRestController {

    private final CandleService candleService;

    @GetMapping("/api/candles")
    public List<Candle> getCandles(@Valid @ModelAttribute CandleQuery query) {
        return candleService.getCandles(
                query.getTicker(),
                String.valueOf(query.getInterval()),
                query.getLimit(),
                query.getFrom(),
                query.getTo()
        );
    }
}