package com.example.tradingplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChandelierExitPoint {
    private Object time;
    private Double longStop;
    private Double shortStop;
    private int direction;
    private boolean buySignal;
    private boolean sellSignal;
}
