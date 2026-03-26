package com.example.tradingplatform.service.papertrading.risk;

import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.PaperTradingRiskModelType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a paper trading position sizer by risk model type.
 */
@Service
public class PaperTradingPositionSizerResolver {

    /** Registry of position sizers keyed by supported risk model */
    private final Map<PaperTradingRiskModelType, PaperTradingPositionSizer> sizers =
            new EnumMap<>(PaperTradingRiskModelType.class);

    /**
     * Registers all available position sizer implementations.
     *
     * @param positionSizers injected sizer implementations
     */
    public PaperTradingPositionSizerResolver(List<PaperTradingPositionSizer> positionSizers) {
        for (PaperTradingPositionSizer positionSizer : positionSizers) {
            sizers.put(positionSizer.getSupportedRiskModel(), positionSizer);
        }
    }

    /**
     * Resolves the position sizer for the given risk model.
     *
     * @param riskModelType requested risk model
     * @return matching position sizer
     */
    public PaperTradingPositionSizer resolve(PaperTradingRiskModelType riskModelType) {
        if (riskModelType == null) {
            throw new InvalidRequestException("Paper trading risk model type is required");
        }

        PaperTradingPositionSizer positionSizer = sizers.get(riskModelType);

        if (positionSizer == null) {
            throw new InvalidRequestException(
                    "Unsupported paper trading risk model: " + riskModelType
            );
        }

        return positionSizer;
    }
}