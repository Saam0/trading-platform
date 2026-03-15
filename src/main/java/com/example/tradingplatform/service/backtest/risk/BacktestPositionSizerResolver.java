package com.example.tradingplatform.service.backtest.risk;

import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.BacktestRiskModelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BacktestPositionSizerResolver {

    private final Map<BacktestRiskModelType, BacktestPositionSizer> sizers;

    public BacktestPositionSizerResolver(List<BacktestPositionSizer> sizerList) {
        this.sizers = new EnumMap<>(BacktestRiskModelType.class);

        for (BacktestPositionSizer sizer : sizerList) {
            this.sizers.put(sizer.getType(), sizer);
        }
    }

    public BacktestPositionSizer resolve(BacktestRiskModelType type) {
        BacktestRiskModelType effectiveType = type == null
                ? BacktestRiskModelType.FULL_CAPITAL
                : type;

        BacktestPositionSizer sizer = sizers.get(effectiveType);

        if (sizer == null) {
            throw new InvalidRequestException("Unsupported risk model: " + effectiveType);
        }

        return sizer;
    }
}