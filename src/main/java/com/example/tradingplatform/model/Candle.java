package com.example.tradingplatform.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Candle {
    private Object time;
    private double open;
    private double high;
    private double low;
    private double close;
    private  double volume;
}
