package com.example.tradingplatform.service.candle;

import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.Candle;
import lombok.AllArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataServiceRaw;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Primary
@Service
@AllArgsConstructor
public class XChangeCandleService implements CandleService {

    private final ExchangeFactoryService exchangeFactoryService;

    @Override
    public List<Candle> getCandles(String ticker, String interval, int limit, Long from, Long to) {
        try {
            Exchange exchange = exchangeFactoryService.createBinanceExchange();
            CurrencyPair currencyPair = exchangeFactoryService.toCurrencyPair(ticker);
            KlineInterval klineInterval = mapInterval(interval);

            BinanceMarketDataServiceRaw marketDataService =
                    (BinanceMarketDataServiceRaw) exchange.getMarketDataService();

            List<BinanceKline> klines = marketDataService.klines(
                    currencyPair,
                    klineInterval,
                    limit,
                    from,
                    to
            );

            List<Candle> candles = new ArrayList<>();

            for (BinanceKline kline : klines) {
                candles.add(mapToCandle(kline, interval));
            }

            return candles;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load candles from Binance", exception);
        }
    }

    private KlineInterval mapInterval(String interval) {
        String normalized = interval.toLowerCase();

        return switch (normalized) {
            case "m1" -> KlineInterval.m1;
            case "m5" -> KlineInterval.m5;
            case "m15" -> KlineInterval.m15;
            case "h1" -> KlineInterval.h1;
            case "h4" -> KlineInterval.h4;
            case "d1" -> KlineInterval.d1;
            default -> throw new InvalidRequestException("Unsupported interval: " + interval);
        };
    }

    private Candle mapToCandle(BinanceKline kline, String interval) {
        Object time;

        if ("d1".equalsIgnoreCase(interval)) {
            time = Instant.ofEpochMilli(kline.getOpenTime())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString();
        } else {
            time = Instant.ofEpochMilli(kline.getOpenTime()).getEpochSecond();
        }

        return new Candle(
                time,
                kline.getOpen().doubleValue(),
                kline.getHigh().doubleValue(),
                kline.getLow().doubleValue(),
                kline.getClose().doubleValue(),
                kline.getVolume().doubleValue()
        );
    }
}