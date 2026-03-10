package com.example.tradingplatform.service;

import com.example.tradingplatform.exchange.ExchangeFactoryService;
import com.example.tradingplatform.model.Candle;
import lombok.AllArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataServiceRaw;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.CandleStick;
import org.knowm.xchange.dto.marketdata.CandleStickData;
import org.knowm.xchange.service.trade.params.CandleStickDataParams;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class XChangeCandleService {
    private final ExchangeFactoryService exchangeFactoryService;

    public List<Candle> getCandles(String ticker) {
        try {
            Exchange exchange = exchangeFactoryService.createBinanceExchange();
            CurrencyPair currencyPair = exchangeFactoryService.toCurrencyPair(ticker);

            BinanceMarketDataServiceRaw marketDataService =
                    (BinanceMarketDataServiceRaw) exchange.getMarketDataService();

            List<BinanceKline> klines = marketDataService.klines(
                    currencyPair,
                    KlineInterval.d1,
                    30,
                    null,
                    null
            );

            CandleStickData candleStickData =
                    BinanceAdapters.adaptBinanceCandleStickData(klines, currencyPair);

            List<Candle> candles = new ArrayList<>();

            for (CandleStick candleStick : candleStickData.getCandleSticks()) {
                candles.add(mapToCandle(candleStick));
            }

            return candles;

        } catch (IOException exception) {
            throw new RuntimeException("Failed to load candles from Binance", exception);
        }
    }
    private Candle mapToCandle(CandleStick candleStick) {
        String date = candleStick.getTimestamp()
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toString();

        return new Candle(
                date,
                candleStick.getOpen().doubleValue(),
                candleStick.getHigh().doubleValue(),
                candleStick.getLow().doubleValue(),
                candleStick.getClose().doubleValue(),
                candleStick.getVolume().doubleValue()
        );
    }
}
