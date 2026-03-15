package com.example.tradingplatform.service;

import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.BacktestStrategyType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BacktestStrategyResolver {

    private final Map<BacktestStrategyType, BacktestStrategy> strategies;

    public BacktestStrategyResolver(List<BacktestStrategy> strategyList) {
        this.strategies = new EnumMap<>(BacktestStrategyType.class);

        for (BacktestStrategy strategy : strategyList) {
            this.strategies.put(strategy.getType(), strategy);
        }
    }

    public BacktestStrategy resolve(BacktestStrategyType type) {
        BacktestStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new InvalidRequestException("Unsupported backtest strategy: " + type);
        }

        return strategy;
    }
}