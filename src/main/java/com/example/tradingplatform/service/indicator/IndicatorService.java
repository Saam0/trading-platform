package com.example.tradingplatform.service.indicator;

import com.example.tradingplatform.dto.ChandelierExitPoint;
import com.example.tradingplatform.dto.IndicatorPoint;

import java.util.List;

public interface IndicatorService {
    List<IndicatorPoint> getSma(String ticker, String interval, int period, int limit, Long to);

    List<IndicatorPoint> getEma(String ticker, String interval, int period, int limit, Long to);

    List<IndicatorPoint> getAtr(String ticker, String interval, int period, int limit, Long to);

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