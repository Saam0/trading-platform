package com.example.tradingplatform.service.stoploss;

import com.example.tradingplatform.dto.stoploss.StopLossRequest;
import com.example.tradingplatform.dto.stoploss.StopLossResult;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.stoploss.StopLossType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves and delegates stop loss generation to proper generator.
 */
@Service
public class StopLossService {

    private final Map<StopLossType, StopLossGenerator> generatorsByType;

    public StopLossService(List<StopLossGenerator> generators) {
        this.generatorsByType = new EnumMap<>(StopLossType.class);

        for (StopLossGenerator generator : generators) {
            generatorsByType.put(generator.supports(), generator);
        }
    }

    /**
     * Generates stop loss using request type.
     *
     * @param request stop loss request
     * @return stop loss result
     */
    public StopLossResult generateStopLoss(StopLossRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Stop loss request is required");
        }

        if (request.getType() == null) {
            throw new InvalidRequestException("Stop loss type is required");
        }

        StopLossGenerator generator = generatorsByType.get(request.getType());

        if (generator == null) {
            throw new InvalidRequestException("Unsupported stop loss type: " + request.getType());
        }

        return generator.generate(request);
    }
}