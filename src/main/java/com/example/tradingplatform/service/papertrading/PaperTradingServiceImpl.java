package com.example.tradingplatform.service.papertrading;

import com.example.tradingplatform.dto.PaperTradingStartRequest;
import com.example.tradingplatform.dto.PaperTradingStatusDto;
import com.example.tradingplatform.exception.InvalidRequestException;
import com.example.tradingplatform.model.PaperTradingSessionStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Default implementation of PaperTradingService.
 *
 * <p>This service manages the lifecycle of the in-memory paper trading session.</p>
 */
@Service
public class PaperTradingServiceImpl implements PaperTradingService {

    /** Strategy resolver used to validate/resolve strategy codes */
    private final TradingStrategyResolver tradingStrategyResolver;

    /** Broker used for simulated execution */
    private final PaperBroker paperBroker;

    /** Single in-memory session state for the current phase */
    private final PaperTradingSession session = new PaperTradingSession();

    /**
     * Creates the service with required collaborators.
     *
     * @param tradingStrategyResolver strategy resolver
     * @param paperBroker broker implementation
     */
    public PaperTradingServiceImpl(
            TradingStrategyResolver tradingStrategyResolver,
            PaperBroker paperBroker
    ) {
        this.tradingStrategyResolver = tradingStrategyResolver;
        this.paperBroker = paperBroker;
    }

    /**
     * Starts a new paper trading session.
     *
     * <p>At this phase the method only initializes the session state.
     * Automatic candle processing is added in the next phase.</p>
     *
     * @param request start request
     * @return current status dto
     */
    @Override
    public synchronized PaperTradingStatusDto start(PaperTradingStartRequest request) {
        // validate that the requested strategy exists
        tradingStrategyResolver.resolve(request.getStrategyCode());

        PaperTradingAccountState accountState = new PaperTradingAccountState();
        accountState.setInitialBalance(request.getInitialBalance());
        accountState.setCurrentBalance(request.getInitialBalance());
        accountState.setFeePercent(request.getFeePercent());
        accountState.setLeverage(request.getLeverage());
        accountState.setClosedTrades(new ArrayList<>());

        // initialize the session metadata
        session.setStatus(PaperTradingSessionStatus.RUNNING);
        session.setTicker(request.getTicker());
        session.setInterval(request.getInterval());
        session.setStrategyCode(request.getStrategyCode().toUpperCase());
        session.setAccountState(accountState);
        session.setLastEventMessage("Paper trading session started");

        return toStatusDto();
    }

    /**
     * Stops the current paper trading session.
     *
     * @return current status dto
     */
    @Override
    public synchronized PaperTradingStatusDto stop() {
        if (session.getStatus() == PaperTradingSessionStatus.STOPPED) {
            throw new InvalidRequestException("Paper trading session is already stopped");
        }

        session.setStatus(PaperTradingSessionStatus.STOPPED);
        session.setLastEventMessage("Paper trading session stopped");

        return toStatusDto();
    }

    /**
     * Returns the current paper trading session status.
     *
     * @return status dto
     */
    @Override
    public synchronized PaperTradingStatusDto getStatus() {
        return toStatusDto();
    }

    /**
     * Returns the internal in-memory session.
     *
     * @return current session
     */
    @Override
    public synchronized PaperTradingSession getSession() {
        return session;
    }

    /**
     * Maps the internal session state to a status dto.
     *
     * @return mapped status dto
     */
    private PaperTradingStatusDto toStatusDto() {
        if (session.getAccountState() == null) {
            return new PaperTradingStatusDto(
                    session.getStatus(),
                    session.getTicker(),
                    session.getInterval(),
                    session.getStrategyCode(),
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    null,
                    new ArrayList<>(),
                    session.getLastEventMessage()
            );
        }

        return new PaperTradingStatusDto(
                session.getStatus(),
                session.getTicker(),
                session.getInterval(),
                session.getStrategyCode(),
                session.getAccountState().getInitialBalance(),
                session.getAccountState().getCurrentBalance(),
                session.getAccountState().getFeePercent(),
                session.getAccountState().getLeverage(),
                session.getAccountState().getOpenPosition(),
                session.getAccountState().getClosedTrades(),
                session.getLastEventMessage()
        );
    }
}