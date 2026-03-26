package com.example.tradingplatform.controller;

import com.example.tradingplatform.dto.PaperTradingRiskPreviewDto;
import com.example.tradingplatform.dto.PaperTradingRiskPreviewRequest;
import com.example.tradingplatform.service.papertrading.risk.StopRiskPositionCalculationService;
import com.example.tradingplatform.service.papertrading.risk.StopRiskPositionSizingResult;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for paper trading risk sizing preview calculations.
 */
@RestController
@AllArgsConstructor
public class PaperTradingRiskPreviewRestController {

    /** Service used for stop-based risk sizing calculation */
    private final StopRiskPositionCalculationService stopRiskPositionCalculationService;

    /**
     * Previews stop-based position sizing values.
     *
     * @param request preview request
     * @return preview result dto
     */
    @PostMapping("/api/paper-trading/risk/preview")
    public PaperTradingRiskPreviewDto previewStopRisk(
            @Valid @RequestBody PaperTradingRiskPreviewRequest request
    ) {
        StopRiskPositionSizingResult result = stopRiskPositionCalculationService.calculate(
                request.getBalance(),
                request.getRiskPercent(),
                request.getFeePercent(),
                request.getEntryPrice(),
                request.getStopPrice()
        );

        return new PaperTradingRiskPreviewDto(
                result.getRiskAmount(),
                result.getStopDistance(),
                result.getStopDistancePercent(),
                result.getEntryFeePercent(),
                result.getExitFeePercent(),
                result.getTotalFeePercent(),
                result.getEffectiveRiskPercent(),
                result.getPositionSize(),
                result.getQuantity(),
                result.getRequiredLeverage()
        );
    }
}