package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.stoploss.StopLossRequest;
import com.example.tradingplatform.dto.stoploss.StopLossResult;
import com.example.tradingplatform.service.stoploss.StopLossResolver;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for stop loss preview calculations.
 */
@Validated
@RestController
@AllArgsConstructor
public class StopLossRestController {

    private final StopLossResolver stopLossResolver;

    /**
     * Previews stop loss result without opening a trade.
     *
     * @param request stop loss request parameters
     * @return calculated stop loss result
     */
    @GetMapping("/api/stop-loss/preview")
    public StopLossResult previewStopLoss(@Valid @ModelAttribute StopLossRequest request) {
        return stopLossResolver.resolve(request);
    }
}