package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.ChandelierExitPoint;
import com.example.tradingplatform.dto.SmaPoint;

import java.util.List;

public interface IndicatorService {
    List<SmaPoint> getSma(String ticker, String interval, int period);
    List<SmaPoint> getEma(String ticker, String interval, int period);
    List<ChandelierExitPoint> getChandelierExit(
            String ticker,
            String interval,
            int length,
            double multiplier,
            boolean useClose
    );
}
