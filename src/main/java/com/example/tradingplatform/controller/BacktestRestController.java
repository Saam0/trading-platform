package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.service.backtest.BacktestService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BacktestRestController {

    private final BacktestService backtestService;

    @PostMapping("/api/backtest/chandelier-exit")
    public BacktestResultDto runBacktest(@Valid @RequestBody BacktestRequest request) {
        return backtestService.runBacktest(request);
    }
}