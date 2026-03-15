document.addEventListener('DOMContentLoaded', function () {
    const modeSelect = document.getElementById('btModeSelect');
    const limitInput = document.getElementById('btLimitInput');
    const fromInput = document.getElementById('btFromInput');
    const toInput = document.getElementById('btToInput');
    const capitalInput = document.getElementById('btCapitalInput');
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
    const totalPnlPercentLabel = document.getElementById('btTotalPnlPercent');

    const tradesBody = document.getElementById('btTradesBody');

    function updateModeUi() {
        const mode = modeSelect.value;
        const isCountMode = mode === 'count';

        limitInput.disabled = !isCountMode;
        fromInput.disabled = isCountMode;
        toInput.disabled = isCountMode;
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
            '<tr><td colspan="10" class="text-center text-muted">' + message + '</td></tr>';
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

    function datetimeLocalToMillis(value) {
        if (!value) {
            return null;
        }

        const millis = new Date(value).getTime();
        return Number.isNaN(millis) ? null : millis;
    }

    function buildRequestBody() {
        const mode = modeSelect.value;
        const ticker = document.getElementById('tickerSelect').value;
        const interval = document.getElementById('intervalSelect').value;

        const requestBody = {
            ticker: ticker,
            interval: interval,
            length: Number(lengthInput.value),
            multiplier: Number(multiplierInput.value),
            useClose: useCloseInput.checked,
            initialCapital: Number(capitalInput.value)
        };

        if (mode === 'count') {
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

    function toBacktestMarkers(trades) {
        const markers = [];

        (trades || []).forEach(function (trade) {
            markers.push({
                time: trade.entryTime,
                position: 'belowBar',
                color: '#1e88e5',
                shape: 'arrowUp',
                text: 'Entry'
            });

            markers.push({
                time: trade.exitTime,
                position: 'aboveBar',
                color: '#f4511e',
                shape: 'arrowDown',
                text: 'Exit'
            });
        });

        return markers;
    }

    function renderSummary(result) {
        summaryEmpty.classList.add('d-none');
        summaryBlock.classList.remove('d-none');

        strategyLabel.textContent = formatValue(result.strategy);
        rangeLabel.textContent = formatValue(result.startTime) + ' → ' + formatValue(result.endTime);
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
        totalPnlLabel.textContent = formatNumber(result.totalPnl);
        initialCapitalLabel.textContent = formatMoney(result.initialCapital);
        finalCapitalLabel.textContent = formatMoney(result.finalCapital);
        netProfitLabel.textContent = formatMoney(result.netProfit);
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
                + '<td>' + formatValue(trade.entryTime) + '</td>'
                + '<td>' + formatNumber(trade.entryPrice) + '</td>'
                + '<td>' + formatValue(trade.exitTime) + '</td>'
                + '<td>' + formatNumber(trade.exitPrice) + '</td>'
                + '<td class="' + pnlClass + '">' + formatNumber(trade.pnl) + '</td>'
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
                renderSummary(result);
                renderTrades(result.trades);

                const markers = toBacktestMarkers(result.trades);

                if (!window.tradingChartApi) {
                    return Promise.resolve();
                }

                if (modeSelect.value === 'range'
                    && typeof window.tradingChartApi.loadBacktestRangeIntoChart === 'function') {
                    return window.tradingChartApi.loadBacktestRangeIntoChart({
                        from: requestBody.from,
                        to: requestBody.to,
                        limit: requestBody.limit
                    }).then(function () {
                        window.tradingChartApi.setBacktestMarkers(markers);
                    });
                }

                if (typeof window.tradingChartApi.setBacktestMarkers === 'function') {
                    window.tradingChartApi.setBacktestMarkers(markers);
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
    clearTradesTable('No backtest results yet.');

    modeSelect.addEventListener('change', updateModeUi);
    runButton.addEventListener('click', runBacktest);
});