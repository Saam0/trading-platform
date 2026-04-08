document.addEventListener('DOMContentLoaded', function () {
    const startBtn = document.getElementById('ptStartBtn');
    const stopBtn = document.getElementById('ptStopBtn');
    const nextCycleBtn = document.getElementById('ptNextCycleBtn');
    const refreshBtn = document.getElementById('ptRefreshBtn');

    const loadingMessage = document.getElementById('ptLoadingMessage');
    const errorMessage = document.getElementById('ptErrorMessage');

    const statusEmpty = document.getElementById('ptStatusEmpty');
    const statusBlock = document.getElementById('ptStatusBlock');

    const openPositionEmpty = document.getElementById('ptOpenPositionEmpty');
    const openPositionBlock = document.getElementById('ptOpenPositionBlock');
    const closedTradesBody = document.getElementById('ptClosedTradesBody');

    const tickerSelect = document.getElementById('tickerSelect');
    const intervalSelect = document.getElementById('intervalSelect');

    const ptTickerInput = document.getElementById('ptTickerInput');
    const ptIntervalSelect = document.getElementById('ptIntervalSelect');
    const ptRiskModelSelect = document.getElementById('ptRiskModelSelect');
    const ptRiskPercentInput = document.getElementById('ptRiskPercentInput');
    const ptPlannedEntryInput = document.getElementById('ptPlannedEntryInput');
    const ptPlannedStopInput = document.getElementById('ptPlannedStopInput');
    const ptExitModelSelect = document.getElementById('ptExitModelSelect');
    const ptRiskRewardRatioInput = document.getElementById('ptRiskRewardRatioInput');

    function showLoading(message) {
        loadingMessage.textContent = message || 'Processing paper trading request...';
        loadingMessage.classList.remove('d-none');
        errorMessage.classList.add('d-none');
    }

    function hideLoading() {
        loadingMessage.classList.add('d-none');
    }

    function showError(message) {
        errorMessage.textContent = message || 'Paper trading request failed.';
        errorMessage.classList.remove('d-none');
    }

    function clearError() {
        errorMessage.classList.add('d-none');
    }

    function formatNumber(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }

        const numericValue = Number(value);

        if (Number.isNaN(numericValue)) {
            return String(value);
        }

        return numericValue.toLocaleString(undefined, {
            minimumFractionDigits: 0,
            maximumFractionDigits: 6
        });
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    function setHtml(id, html) {
        const element = document.getElementById(id);
        if (element) {
            element.innerHTML = html;
        }
    }

    function readOptionalNumber(inputId) {
        const value = document.getElementById(inputId).value.trim();

        if (value === '') {
            return null;
        }

        return Number(value);
    }

    function syncFromChartControls() {
        if (tickerSelect && ptTickerInput) {
            ptTickerInput.value = tickerSelect.value;
        }

        if (intervalSelect && ptIntervalSelect) {
            ptIntervalSelect.value = intervalSelect.value;
        }
    }

    function updateRiskModelFields() {
        const isStopRisk = ptRiskModelSelect.value === 'STOP_RISK_PERCENT';

        ptRiskPercentInput.disabled = !isStopRisk;
        ptPlannedEntryInput.disabled = !isStopRisk;
        ptPlannedStopInput.disabled = !isStopRisk;

        if (!isStopRisk) {
            ptPlannedEntryInput.value = '';
            ptPlannedStopInput.value = '';
        }
    }

    function updateExitModelFields() {
        const isFixedRr = ptExitModelSelect.value === 'FIXED_RR';
        ptRiskRewardRatioInput.disabled = !isFixedRr;
    }

    function formatEventMessage(message) {
        if (!message) {
            return '-';
        }

        return message
            .replaceAll('_', ' ')
            .replace(/\s+/g, ' ')
            .trim();
    }

    function buildStartRequest() {
        return {
            ticker: ptTickerInput.value.trim(),
            interval: ptIntervalSelect.value,
            strategyCode: document.getElementById('ptStrategyCodeInput').value.trim(),
            initialBalance: Number(document.getElementById('ptInitialBalanceInput').value),
            feePercent: Number(document.getElementById('ptFeePercentInput').value),
            leverage: Number(document.getElementById('ptLeverageInput').value),
            riskModelType: ptRiskModelSelect.value,
            riskPercent: Number(ptRiskPercentInput.value),
            plannedEntryPrice: readOptionalNumber('ptPlannedEntryInput'),
            plannedStopPrice: readOptionalNumber('ptPlannedStopInput'),
            exitModelType: ptExitModelSelect.value,
            riskRewardRatio: Number(ptRiskRewardRatioInput.value)
        };
    }

    function badgeHtml(text, badgeClass) {
        return '<span class="badge ' + badgeClass + '">' + text + '</span>';
    }

    function statusBadge(status) {
        if (status === 'RUNNING') {
            return badgeHtml('RUNNING', 'text-bg-success');
        }

        if (status === 'STOPPED') {
            return badgeHtml('STOPPED', 'text-bg-secondary');
        }

        return badgeHtml(status || '-', 'text-bg-light');
    }

    function sideBadge(side) {
        if (side === 'LONG') {
            return badgeHtml('LONG', 'text-bg-success');
        }

        if (side === 'SHORT') {
            return badgeHtml('SHORT', 'text-bg-danger');
        }

        return badgeHtml(side || '-', 'text-bg-light');
    }

    function eventBadge(message) {
        if (!message) {
            return badgeHtml('-', 'text-bg-light');
        }

        if (message.startsWith('OPEN_LONG')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-success');
        }

        if (message.startsWith('OPEN_SHORT')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-danger');
        }

        if (message.includes('TAKE_PROFIT')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-success');
        }

        if (message.includes('STOP_LOSS')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-danger');
        }

        if (message.startsWith('HOLD')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-secondary');
        }

        if (message.startsWith('SKIP')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-warning');
        }

        if (message.startsWith('CLOSE_')) {
            return badgeHtml(formatEventMessage(message), 'text-bg-primary');
        }

        return badgeHtml(formatEventMessage(message), 'text-bg-light');
    }

    function signedValueHtml(value) {
        const numericValue = Number(value);

        if (Number.isNaN(numericValue)) {
            return '<span>' + formatNumber(value) + '</span>';
        }

        if (numericValue > 0) {
            return '<span class="text-success fw-semibold">' + formatNumber(numericValue) + '</span>';
        }

        if (numericValue < 0) {
            return '<span class="text-danger fw-semibold">' + formatNumber(numericValue) + '</span>';
        }

        return '<span class="fw-semibold">' + formatNumber(numericValue) + '</span>';
    }

    function neutralValueHtml(value) {
        return '<span class="fw-semibold">' + formatNumber(value) + '</span>';
    }

    function optionalPriceHtml(value) {
        const numericValue = Number(value);

        if (!value || Number.isNaN(numericValue) || numericValue === 0) {
            return '<span class="text-muted">-</span>';
        }

        return '<span class="fw-semibold">' + formatNumber(numericValue) + '</span>';
    }

    function renderClosedTrades(closedTrades) {
        if (!closedTrades || closedTrades.length === 0) {
            closedTradesBody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center text-muted">No closed trades yet.</td>
                </tr>
            `;
            return;
        }

        closedTradesBody.innerHTML = closedTrades.map(function (trade, index) {
            const pnlClass = Number(trade.pnl) > 0
                ? 'text-success fw-semibold'
                : Number(trade.pnl) < 0
                    ? 'text-danger fw-semibold'
                    : 'fw-semibold';

            return `
                <tr>
                    <td>${index + 1}</td>
                    <td>${sideBadge(trade.side ?? '-')}</td>
                    <td>${formatNumber(trade.entryPrice)}</td>
                    <td>${formatNumber(trade.exitPrice)}</td>
                    <td>${formatNumber(trade.quantity)}</td>
                    <td>${formatNumber(trade.positionSize)}</td>
                    <td>${formatNumber(trade.fee)}</td>
                    <td><span class="${pnlClass}">${formatNumber(trade.pnl)}</span></td>
                    <td>${formatEventMessage(trade.reason ?? '-')}</td>
                </tr>
            `;
        }).join('');
    }

    function renderOpenPosition(openPosition) {
        if (!openPosition) {
            openPositionEmpty.classList.remove('d-none');
            openPositionBlock.classList.add('d-none');
            return;
        }

        openPositionEmpty.classList.add('d-none');
        openPositionBlock.classList.remove('d-none');

        setHtml('ptOpenSideValue', sideBadge(openPosition.side ?? '-'));
        setHtml('ptOpenEntryPriceValue', neutralValueHtml(openPosition.entryPrice));
        setHtml('ptOpenQuantityValue', neutralValueHtml(openPosition.quantity));
        setHtml('ptOpenPositionSizeValue', neutralValueHtml(openPosition.positionSize));
        setHtml('ptOpenStopPriceValue', optionalPriceHtml(openPosition.stopPrice));
        setHtml('ptOpenTargetPriceValue', optionalPriceHtml(openPosition.targetPrice));
    }

    function renderStatus(status) {
        statusEmpty.classList.add('d-none');
        statusBlock.classList.remove('d-none');

        setHtml('ptStatusValue', statusBadge(status.status));
        setText('ptTickerIntervalValue', (status.ticker ?? '-') + ' / ' + (status.interval ?? '-'));
        setText('ptStrategyValue', status.strategyCode ?? '-');
        setHtml('ptLastEventValue', eventBadge(status.lastEventMessage));

        setHtml('ptInitialBalanceValue', neutralValueHtml(status.initialBalance));
        setHtml('ptCurrentBalanceValue', neutralValueHtml(status.currentBalance));
        setHtml('ptCurrentPriceValue', neutralValueHtml(status.currentPrice));
        setHtml('ptEquityValue', neutralValueHtml(status.equity));
        setHtml('ptUnrealizedPnlValue', signedValueHtml(status.unrealizedPnl));
        setHtml('ptTotalPnlValue', signedValueHtml(status.totalPnl));
        setHtml('ptFeePercentValue', neutralValueHtml(status.feePercent));
        setHtml('ptLeverageValue', neutralValueHtml(status.leverage));
        setHtml('ptRequiredLeverageValue', neutralValueHtml(status.requiredLeverage));

        renderOpenPosition(status.openPosition);
        renderClosedTrades(status.closedTrades);
    }

    function handleJsonResponse(response) {
        if (!response.ok) {
            return response.json()
                .then(function (errorBody) {
                    throw new Error(errorBody.message || 'Request failed');
                })
                .catch(function () {
                    throw new Error('Request failed');
                });
        }

        return response.json();
    }

    function refreshStatus() {
        showLoading('Loading paper trading status...');

        fetch('/api/paper-trading/status')
            .then(handleJsonResponse)
            .then(function (status) {
                renderStatus(status);
                clearError();
            })
            .catch(function (error) {
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    function startSession() {
        const requestBody = buildStartRequest();

        showLoading('Starting paper trading session...');

        fetch('/api/paper-trading/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        })
            .then(handleJsonResponse)
            .then(function (status) {
                renderStatus(status);
                clearError();
            })
            .catch(function (error) {
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    function stopSession() {
        showLoading('Stopping paper trading session...');

        fetch('/api/paper-trading/stop', {
            method: 'POST'
        })
            .then(handleJsonResponse)
            .then(function (status) {
                renderStatus(status);
                clearError();
            })
            .catch(function (error) {
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    function runNextCycle() {
        showLoading('Running next paper trading cycle...');

        fetch('/api/paper-trading/engine/next-cycle', {
            method: 'POST'
        })
            .then(handleJsonResponse)
            .then(function (status) {
                renderStatus(status);
                clearError();
            })
            .catch(function (error) {
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    if (tickerSelect) {
        tickerSelect.addEventListener('change', syncFromChartControls);
    }

    if (intervalSelect) {
        intervalSelect.addEventListener('change', syncFromChartControls);
    }

    ptRiskModelSelect.addEventListener('change', updateRiskModelFields);
    ptExitModelSelect.addEventListener('change', updateExitModelFields);

    startBtn.addEventListener('click', startSession);
    stopBtn.addEventListener('click', stopSession);
    nextCycleBtn.addEventListener('click', runNextCycle);
    refreshBtn.addEventListener('click', refreshStatus);

    syncFromChartControls();
    updateRiskModelFields();
    updateExitModelFields();
    refreshStatus();
});