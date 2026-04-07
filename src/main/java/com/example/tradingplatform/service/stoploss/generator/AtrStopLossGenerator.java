package com.example.tradingplatform.service.stoploss.generator;

import com.example.tradingplatform.dto.stoploss.StopLossRequest;
import com.example.tradingplatform.dto.stoploss.StopLossResult;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.stoploss.StopLossType;
import com.example.tradingplatform.service.stoploss.StopLossGenerator;
import org.springframework.stereotype.Service;

/**
 * Stop loss generator based on ATR distance.
 */
@Service
public class AtrStopLossGenerator implements StopLossGenerator {

    @Override
    public StopLossType supports() {
        return StopLossType.ATR;
    }

    @Override
    public StopLossResult generate(StopLossRequest request) {
        validate(request);

        double entryPrice = request.getEntryPrice();
        double distance = request.getAtrValue() * request.getAtrMultiplier();

        double stopPrice = switch (request.getSide()) {
            case LONG -> entryPrice - distance;
            case SHORT -> entryPrice + distance;
        };

        double distancePercent = (distance / entryPrice) * 100.0;

        return new StopLossResult(
                StopLossType.ATR,
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

        if (request.getAtrValue() == null) {
            throw new InvalidRequestException("ATR value is required");
        }

        if (request.getAtrValue() <= 0) {
            throw new InvalidRequestException("ATR value must be greater than zero");
        }

        if (request.getAtrMultiplier() == null) {
            throw new InvalidRequestException("ATR multiplier is required");
        }

        if (request.getAtrMultiplier() <= 0) {
            throw new InvalidRequestException("ATR multiplier must be greater than zero");
        }
    }
}