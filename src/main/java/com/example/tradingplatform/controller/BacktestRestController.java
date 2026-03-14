package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import com.example.tradingplatform.service.BacktestService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class BacktestRestController {

    private final BacktestService backtestService;

    @PostMapping("/api/backtest/chandelier-exit")
    public BacktestResultDto runChandelierExitBacktest(
            @Valid @RequestBody BacktestRequest request
    ) {
        return backtestService.runChandelierExitBacktest(request);
    }

}