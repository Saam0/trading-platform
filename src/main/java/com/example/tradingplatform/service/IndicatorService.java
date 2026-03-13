package com.example.tradingplatform.service;

import com.example.tradingplatform.dto.ChandelierExitPoint;
import com.example.tradingplatform.dto.SmaPoint;

import java.util.List;

public interface IndicatorService {
    List<SmaPoint> getSma(String ticker, String interval, int period,  int limit, Long to);

    List<SmaPoint> getEma(String ticker, String interval, int period, int limit, Long to);

    List<ChandelierExitPoint> getChandelierExit(
            String ticker,
            String interval,
            int length,
            double multiplier,
            boolean useClose,
            int limit,
            Long to
    );
}
