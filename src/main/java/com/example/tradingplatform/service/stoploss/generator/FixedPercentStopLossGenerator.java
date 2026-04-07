package com.example.tradingplatform.service.stoploss.generator;

import com.example.tradingplatform.dto.stoploss.StopLossRequest;
import com.example.tradingplatform.dto.stoploss.StopLossResult;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.stoploss.StopLossType;
import com.example.tradingplatform.service.stoploss.StopLossGenerator;
import org.springframework.stereotype.Service;

/**
 * Stop loss generator based on fixed percentage distance from entry.
 */
@Service
public class FixedPercentStopLossGenerator implements StopLossGenerator {

    @Override
    public StopLossType supports() {
        return StopLossType.FIXED_PERCENT;
    }

    @Override
    public StopLossResult generate(StopLossRequest request) {
        validate(request);

        double entryPrice = request.getEntryPrice();
        double percentAsDecimal = request.getFixedPercent() / 100.0;

        double stopPrice = switch (request.getSide()) {
            case LONG -> entryPrice * (1.0 - percentAsDecimal);
            case SHORT -> entryPrice * (1.0 + percentAsDecimal);
        };

        double distance = Math.abs(entryPrice - stopPrice);
        double distancePercent = (distance / entryPrice) * 100.0;

        return new StopLossResult(
                StopLossType.FIXED_PERCENT,
                stopPrice,
                distance,
                distancePercent
        );
    }

    private void validate(StopLossRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Stop loss request is required");
        }

        if (request.getType() == null) {
            throw new InvalidRequestException("Stop loss type is required");
        }

        if (request.getSide() == null) {
            throw new InvalidRequestException("Trade side is required");
        }

        if (request.getEntryPrice() <= 0) {
            throw new InvalidRequestException("Entry price must be greater than zero");
        }

        if (request.getFixedPercent() == null) {
            throw new InvalidRequestException("Fixed percent is required");
        }

        if (request.getFixedPercent() <= 0) {
            throw new InvalidRequestException("Fixed percent must be greater than zero");
        }
    }
}