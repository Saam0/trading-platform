package com.example.tradingplatform.service.stoploss;

import com.example.tradingplatform.dto.stoploss.StopLossRequest;
import com.example.tradingplatform.dto.stoploss.StopLossResult;
import com.example.tradingplatform.model.stoploss.StopLossType;

/**
 * Common contract for stop loss generators.
 */
public interface StopLossGenerator {

    /**
     * Returns supported stop loss type.
     *
     * @return supported type
     */
    StopLossType supports();

    /**
     * Generates stop loss result from request.
     *
     * @param request stop loss request
     * @return stop loss calculation result
     */
    StopLossResult generate(StopLossRequest request);
}