package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves trading strategies by strategy code.
 */
@Service
public class TradingStrategyResolver {

    /** Strategy registry keyed by uppercased strategy code */
    private final Map<String, TradingStrategy> strategies = new HashMap<>();

    /**
     * Registers all available TradingStrategy beans.
     *
     * @param strategyList injected strategy implementations
     */
    public TradingStrategyResolver(List<TradingStrategy> strategyList) {
        for (TradingStrategy strategy : strategyList) {
            // normalize to uppercase for case-insensitive lookup
            strategies.put(strategy.getCode().toUpperCase(), strategy);
        }
    }

    /**
     * Resolves a strategy by code.
     *
     * @param strategyCode user-provided strategy code
     * @return matching strategy
     */
    public TradingStrategy resolve(String strategyCode) {
        if (strategyCode == null || strategyCode.isBlank()) {
            throw new InvalidRequestException("Strategy code is required");
        }

        // normalize the input before map lookup
        TradingStrategy strategy = strategies.get(strategyCode.toUpperCase());

        if (strategy == null) {
            throw new InvalidRequestException("Unsupported trading strategy: " + strategyCode);
        }

        return strategy;
    }
}