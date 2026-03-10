package com.example.tradingplatform.exchange;

import com.example.tradingplatform.exception.InvalidRequestException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ExchangeFactoryService {

    public Exchange createBinanceExchange() throws IOException {
        Exchange exchange = new BinanceExchange();
        ExchangeSpecification specification = exchange.getDefaultExchangeSpecification();
        exchange.applySpecification(specification);
        exchange.remoteInit();
        return exchange;
    }

    public CurrencyPair toCurrencyPair(String ticker) {
        String normalized = ticker.toUpperCase();

        return switch (normalized) {
            case "ETHUSDT" -> new CurrencyPair("ETH", "USDT");
            case "SOLUSDT" -> new CurrencyPair("SOL", "USDT");
            case "BTCUSDT" -> new CurrencyPair("BTC", "USDT");
            default -> throw new InvalidRequestException("Unsupported ticker: " + ticker);
        };
    }
}
