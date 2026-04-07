package com.example.tradingplatform.dto.stoploss;

import com.example.tradingplatform.model.stoploss.StopLossType;
import com.example.tradingplatform.model.TradeSide;

/**
 * Request object for stop loss generation.
 */
public class StopLossRequest {

    private StopLossType type;
    private TradeSide side;
    private double entryPrice;
    private Double fixedPercent;
    private Double atrValue;
    private Double atrMultiplier;

    public StopLossRequest() {
    }

    public StopLossRequest(
            StopLossType type,
            TradeSide side,
            double entryPrice,
            Double fixedPercent,
            Double atrValue,
            Double atrMultiplier
    ) {
        this.type = type;
        this.side = side;
        this.entryPrice = entryPrice;
        this.fixedPercent = fixedPercent;
        this.atrValue = atrValue;
        this.atrMultiplier = atrMultiplier;
    }

    public StopLossType getType() {
        return type;
    }

    public void setType(StopLossType type) {
        this.type = type;
    }

    public TradeSide getSide() {
        return side;
    }

    public void setSide(TradeSide side) {
        this.side = side;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public Double getFixedPercent() {
        return fixedPercent;
    }

    public void setFixedPercent(Double fixedPercent) {
        this.fixedPercent = fixedPercent;
    }

    public Double getAtrValue() {
        return atrValue;
    }

    public void setAtrValue(Double atrValue) {
        this.atrValue = atrValue;
    }

    public Double getAtrMultiplier() {
        return atrMultiplier;
    }

    public void setAtrMultiplier(Double atrMultiplier) {
        this.atrMultiplier = atrMultiplier;
    }
}