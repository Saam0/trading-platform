document.addEventListener('DOMContentLoaded', function () {
    const strategySelect = document.getElementById('btStrategySelect');
    const modeSelect = document.getElementById('btModeSelect');
    const limitInput = document.getElementById('btLimitInput');
    const fromInput = document.getElementById('btFromInput');
    const toInput = document.getElementById('btToInput');

    const riskModelSelect = document.getElementById('btRiskModelSelect');
    const exitModelSelect = document.getElementById('btExitModelSelect');
    const riskRewardSelect = document.getElementById('btRiskRewardSelect');

    const capitalInput = document.getElementById('btCapitalInput');
    const leverageInput = document.getElementById('btLeverageInput');
    const riskInput = document.getElementById('btRiskInput');
    const fixedNotionalInput = document.getElementById('btFixedNotionalInput');
    const feeInput = document.getElementById('btFeeInput');

    const lengthInput = document.getElementById('btLengthInput');
    const multiplierInput = document.getElementById('btMultiplierInput');
    const useCloseInput = document.getElementById('btUseCloseInput');
    const runButton = document.getElementById('runBacktestBtn');

    const loadingMessage = document.getElementById('btLoadingMessage');
    const errorMessage = document.getElementById('btErrorMessage');

    const summaryEmpty = document.getElementById('btSummaryEmpty');
    const summaryBlock = document.getElementById('btSummaryBlock');

    const strategyLabel = document.getElementById('btStrategy');
    const rangeLabel = document.getElementById('btRange');
    const tradesLabel = document.getElementById('btTrades');
    const winRateLabel = document.getElementById('btWinRate');
    const netProfitPercentLabel = document.getElementById('btNetProfitPercent');
    const tickerIntervalLabel = document.getElementById('btTickerInterval');
    const paramsLabel = document.getElementById('btParams');
    const winsLabel = document.getElementById('btWins');
    const lossesLabel = document.getElementById('btLosses');
    const totalPnlLabel = document.getElementById('btTotalPnl');
    const initialCapitalLabel = document.getElementById('btInitialCapital');
    const finalCapitalLabel = document.getElementById('btFinalCapital');
    const netProfitLabel = document.getElementById('btNetProfit');
    const riskModelLabel = document.getElementById('btRiskModel');
    const exitModelLabel = document.getElementById('btExitModel');
    const riskRewardLabel = document.getElementById('btRiskReward');
    const leverageFeeLabel = document.getElementById('btLeverageFee');
    const totalPnlPercentLabel = document.getElementById('btTotalPnlPercent');

    const tradesBody = document.getElementById('btTradesBody');

    function updateModeUi() {
        const isCountMode = modeSelect.value === 'count';
        limitInput.disabled = !isCountMode;
        fromInput.disabled = isCountMode;
        toInput.disabled = isCountMode;
    }

    function updateRiskModelUi() {
        const riskModel = riskModelSelect.value;
        riskInput.disabled = riskModel !== 'RISK_PERCENT';
        fixedNotionalInput.disabled = riskModel !== 'FIXED_NOTIONAL';
    }

    function updateExitModelUi() {
        const exitModel = exitModelSelect.value;
        riskRewardSelect.disabled = exitModel !== 'FIXED_RR_TP';
    }

    function showLoading() {
        loadingMessage.classList.remove('d-none');
        errorMessage.classList.add('d-none');
        runButton.disabled = true;
    }

    function hideLoading() {
        loadingMessage.classList.add('d-none');
        runButton.disabled = false;
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.remove('d-none');
    }

    function hideError() {
        errorMessage.classList.add('d-none');
    }

    function clearTradesTable(message) {
        tradesBody.innerHTML =
            '<tr><td colspan="16" class="text-center text-muted">' + message + '</td></tr>';
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString(undefined, {
            minimumFractionDigits: 0,
            maximumFractionDigits: 2
        });
    }

    function formatMoney(value) {
        return '$' + formatNumber(value);
    }

    function formatPercent(value) {
        return formatNumber(value) + '%';
    }

    function formatValue(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }
        return String(value);
    }

    function pad2(value) {
        return String(value).padStart(2, '0');
    }

    function formatDateObject(date) {
        return date.getFullYear()
            + '-' + pad2(date.getMonth() + 1)
            + '-' + pad2(date.getDate())
            + ' ' + pad2(date.getHours())
            + ':' + pad2(date.getMinutes());
    }

    function formatDateTimeValue(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }

        if (typeof value === 'number') {
            const date = new Date(value * 1000);
            if (!Number.isNaN(date.getTime())) {
                return formatDateObject(date);
            }
        }

        if (typeof value === 'string') {
            if (/^\d+$/.test(value)) {
                const numericValue = Number(value);
                const millis = value.length >= 13 ? numericValue : numericValue * 1000;
                const numericDate = new Date(millis);

                if (!Number.isNaN(numericDate.getTime())) {
                    return formatDateObject(numericDate);
                }
            }

            if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
                return value;
            }

            const parsedDate = new Date(value);
            if (!Number.isNaN(parsedDate.getTime())) {
                return formatDateObject(parsedDate);
            }
        }

        return String(value);
    }

    function formatRangeValue(value) {
        return formatDateTimeValue(value);
    }

    function datetimeLocalToMillis(value) {
        if (!value) {
            return null;
        }

        const millis = new Date(value).getTime();
        return Number.isNaN(millis) ? null : millis;
    }

    function buildRequestBody() {
        const requestBody = {
            ticker: document.getElementById('tickerSelect').value,
            interval: document.getElementById('intervalSelect').value,
            strategyType: strategySelect.value,
            riskModelType: riskModelSelect.value,
            exitModelType: exitModelSelect.value,
            riskRewardRatio: Number(riskRewardSelect.value),
            initialCapital: Number(capitalInput.value),
            leverage: Number(leverageInput.value),
            riskPercent: Number(riskInput.value),
            fixedNotional: Number(fixedNotionalInput.value),
            feePercent: Number(feeInput.value),
            length: Number(lengthInput.value),
            multiplier: Number(multiplierInput.value),
            useClose: useCloseInput.checked
        };

        if (modeSelect.value === 'count') {
            requestBody.limit = Number(limitInput.value);
            return requestBody;
        }

        const from = datetimeLocalToMillis(fromInput.value);
        const to = datetimeLocalToMillis(toInput.value);

        if (from === null || to === null) {
            throw new Error('Range mode-ի համար From և To պարտադիր են');
        }

        if (from >= to) {
            throw new Error('From-ը պետք է փոքր լինի To-ից');
        }

        requestBody.from = from;
        requestBody.to = to;
        requestBody.limit = 1000;

        return requestBody;
    }

    function syncChartIndicatorControls(requestBody) {
        const showCeToggle = document.getElementById('showCeToggle');
        const ceLengthSelect = document.getElementById('ceLengthSelect');
        const ceMultiplierInput = document.getElementById('ceMultiplierInput');
        const ceUseCloseToggle = document.getElementById('ceUseCloseToggle');

        if (showCeToggle) {
            showCeToggle.checked = true;
        }

        if (ceLengthSelect) {
            ceLengthSelect.value = String(requestBody.length);
        }

        if (ceMultiplierInput) {
            ceMultiplierInput.value = String(requestBody.multiplier);
        }

        if (ceUseCloseToggle) {
            ceUseCloseToggle.checked = requestBody.useClose;
        }
    }

    function toBacktestMarkers(trades) {
        const markers = [];

        (trades || []).forEach(function (trade) {
            const isLong = trade.side === 'LONG';

            markers.push({
                time: trade.entryTime,
                position: isLong ? 'belowBar' : 'aboveBar',
                color: isLong ? '#1e88e5' : '#8e24aa',
                shape: isLong ? 'arrowUp' : 'arrowDown',
                text: isLong ? 'Long Entry' : 'Short Entry'
            });

            markers.push({
                time: trade.exitTime,
                position: isLong ? 'aboveBar' : 'belowBar',
                color: isLong ? '#f4511e' : '#43a047',
                shape: isLong ? 'arrowDown' : 'arrowUp',
                text: isLong ? 'Long Exit' : 'Short Exit'
            });
        });

        return markers;
    }

    function renderSummary(result, requestBody) {
        summaryEmpty.classList.add('d-none');
        summaryBlock.classList.remove('d-none');

        strategyLabel.textContent = formatValue(result.strategy);
        rangeLabel.textContent = formatRangeValue(result.startTime) + ' → ' + formatRangeValue(result.endTime);
        tradesLabel.textContent = formatValue(result.totalTrades);
        winRateLabel.textContent = formatPercent(result.winRate);
        netProfitPercentLabel.textContent = formatPercent(result.netProfitPercent);
        tickerIntervalLabel.textContent = formatValue(result.ticker) + ' / ' + formatValue(result.interval);
        paramsLabel.textContent =
            'L=' + formatValue(result.length)
            + ', M=' + formatValue(result.multiplier)
            + ', useClose=' + formatValue(result.useClose);
        winsLabel.textContent = formatValue(result.winningTrades);
        lossesLabel.textContent = formatValue(result.losingTrades);
        totalPnlLabel.textContent = formatMoney(result.totalPnl);
        initialCapitalLabel.textContent = formatMoney(result.initialCapital);
        finalCapitalLabel.textContent = formatMoney(result.finalCapital);
        netProfitLabel.textContent = formatMoney(result.netProfit);
        riskModelLabel.textContent = formatValue(requestBody.riskModelType);
        exitModelLabel.textContent = formatValue(requestBody.exitModelType);
        riskRewardLabel.textContent = requestBody.exitModelType === 'FIXED_RR_TP'
            ? '1:' + formatValue(requestBody.riskRewardRatio)
            : '-';
        leverageFeeLabel.textContent =
            formatValue(requestBody.leverage) + 'x / ' + formatPercent(requestBody.feePercent);
        totalPnlPercentLabel.textContent = formatPercent(result.totalPnlPercent);

        netProfitLabel.className =
            Number(result.netProfit || 0) > 0 ? 'fw-semibold text-success'
                : Number(result.netProfit || 0) < 0 ? 'fw-semibold text-danger'
                    : 'fw-semibold';

        netProfitPercentLabel.className =
            Number(result.netProfitPercent || 0) > 0 ? 'fw-semibold text-success'
                : Number(result.netProfitPercent || 0) < 0 ? 'fw-semibold text-danger'
                    : 'fw-semibold';
    }

    function renderTrades(trades) {
        if (!trades || trades.length === 0) {
            clearTradesTable('No trades found for this backtest.');
            return;
        }

        const rows = trades.map(function (trade, index) {
            const pnlClass = Number(trade.pnl) > 0
                ? 'text-success'
                : Number(trade.pnl) < 0
                    ? 'text-danger'
                    : '';

            const pnlPercentClass = Number(trade.pnlPercent) > 0
                ? 'text-success'
                : Number(trade.pnlPercent) < 0
                    ? 'text-danger'
                    : '';

            return ''
                + '<tr>'
                + '<td>' + (index + 1) + '</td>'
                + '<td>' + formatValue(trade.side) + '</td>'
                + '<td>' + formatDateTimeValue(trade.entryTime) + '</td>'
                + '<td>' + formatNumber(trade.entryPrice) + '</td>'
                + '<td>' + formatNumber(trade.quantity) + '</td>'
                + '<td>' + formatMoney(trade.positionSize) + '</td>'
                + '<td>' + formatMoney(trade.fee) + '</td>'
                + '<td>' + formatMoney(trade.capitalBefore) + '</td>'
                + '<td>' + formatMoney(trade.capitalAfter) + '</td>'
                + '<td>' + formatDateTimeValue(trade.exitTime) + '</td>'
                + '<td>' + formatNumber(trade.exitPrice) + '</td>'
                + '<td class="' + pnlClass + '">' + formatMoney(trade.pnl) + '</td>'
                + '<td class="' + pnlPercentClass + '">' + formatPercent(trade.pnlPercent) + '</td>'
                + '<td>' + formatValue(trade.barsHeld) + '</td>'
                + '<td>' + formatValue(trade.result) + '</td>'
                + '<td>' + formatValue(trade.exitReason) + '</td>'
                + '</tr>';
        }).join('');

        tradesBody.innerHTML = rows;
    }

    function runBacktest() {
        let requestBody;

        try {
            requestBody = buildRequestBody();
        } catch (error) {
            showError(error.message);
            return;
        }

        syncChartIndicatorControls(requestBody);

        showLoading();
        hideError();

        fetch('/api/backtest/chandelier-exit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        })
            .then(function (response) {
                if (!response.ok) {
                    return response.json()
                        .then(function (errorBody) {
                            throw new Error(errorBody.message || 'Failed to run backtest');
                        })
                        .catch(function () {
                            throw new Error('Failed to run backtest');
                        });
                }

                return response.json();
            })
            .then(function (result) {
                renderSummary(result, requestBody);
                renderTrades(result.trades);

                const markers = toBacktestMarkers(result.trades);

                if (!window.tradingChartApi) {
                    return Promise.resolve();
                }

                const loadOptions = {
                    from: requestBody.from,
                    to: requestBody.to,
                    limit: requestBody.limit
                };

                if (typeof window.tradingChartApi.loadBacktestRangeIntoChart === 'function') {
                    return window.tradingChartApi.loadBacktestRangeIntoChart(loadOptions)
                        .then(function () {
                            if (typeof window.tradingChartApi.setBacktestMarkers === 'function') {
                                window.tradingChartApi.setBacktestMarkers(markers);
                            }
                        });
                }

                return Promise.resolve();
            })
            .catch(function (error) {
                console.error('Backtest error:', error);
                showError(error.message);
                clearTradesTable('Backtest failed.');
            })
            .finally(function () {
                hideLoading();
            });
    }

    updateModeUi();
    updateRiskModelUi();
    updateExitModelUi();
    clearTradesTable('No backtest results yet.');

    modeSelect.addEventListener('change', updateModeUi);
    riskModelSelect.addEventListener('change', updateRiskModelUi);
    exitModelSelect.addEventListener('change', updateExitModelUi);
    runButton.addEventListener('click', runBacktest);
});