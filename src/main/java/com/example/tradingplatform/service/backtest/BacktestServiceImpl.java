package com.example.tradingplatform.service.backtest;

import com.example.tradingplatform.dto.BacktestRequest;
import com.example.tradingplatform.dto.BacktestResultDto;
import org.springframework.stereotype.Service;

@Service
public class BacktestServiceImpl implements BacktestService {

    private final BacktestStrategyResolver strategyResolver;

    public BacktestServiceImpl(BacktestStrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    @Override
    public BacktestResultDto runBacktest(BacktestRequest request) {
        return strategyResolver
                .resolve(request.getStrategyType())
                .run(request);
    }
}