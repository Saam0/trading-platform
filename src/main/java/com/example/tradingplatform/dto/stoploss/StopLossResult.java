package com.example.tradingplatform.dto.stoploss;

import com.example.tradingplatform.model.stoploss.StopLossType;

/**
 * Result of stop loss generation.
 */
public class StopLossResult {

    private StopLossType type;
    private double stopPrice;
    private double distance;
    private double distancePercent;

    public StopLossResult() {
    }

    public StopLossResult(
            StopLossType type,
            double stopPrice,
            double distance,
            double distancePercent
    ) {
        this.type = type;
        this.stopPrice = stopPrice;
        this.distance = distance;
        this.distancePercent = distancePercent;
    }

    public StopLossType getType() {
        return type;
    }

    public void setType(StopLossType type) {
        this.type = type;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistancePercent() {
        return distancePercent;
    }

    public void setDistancePercent(double distancePercent) {
        this.distancePercent = distancePercent;
    }
}