document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');
    const tickerSelect = document.getElementById('tickerSelect');
    const intervalSelect = document.getElementById('intervalSelect');
    const enableScaleToggle = document.getElementById('enableScaleToggle');
    const enableScrollToggle = document.getElementById('enableScrollToggle');
    const showVolumeToggle = document.getElementById('showVolumeToggle');
    const showSmaToggle = document.getElementById('showSmaToggle');
    const smaPeriodSelect = document.getElementById('smaPeriodSelect');

    const loadingMessage = document.getElementById('loadingMessage');
    const errorMessage = document.getElementById('errorMessage');

    const selectedTickerLabel = document.getElementById('selectedTickerLabel');
    const selectedIntervalLabel = document.getElementById('selectedIntervalLabel');
    const lastCloseLabel = document.getElementById('lastCloseLabel');
    const showSmaLabel = document.getElementById('showSmaLabel');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 650,
        layout: {
            background: { color: '#ffffff' },
            textColor: '#212529',
            panes: {
                separatorColor: '#dee2e6',
                separatorHoverColor: '#adb5bd',
                enableResize: true
            }
        },
        grid: {
            vertLines: { color: '#f1f3f5' },
            horzLines: { color: '#f1f3f5' }
        },
        rightPriceScale: {
            borderColor: '#dee2e6'
        },
        timeScale: {
            borderColor: '#dee2e6',
            timeVisible: true,
            secondsVisible: false,
            barSpacing: 10
        },
        handleScale: {
            mouseWheel: true,
            pinch: true,
            axisPressedMouseMove: {
                time: true,
                price: true
            }
        },
        handleScroll: {
            mouseWheel: true,
            pressedMouseMove: true,
            horzTouchDrag: true,
            vertTouchDrag: true
        }
    });

    let candlestickSeries;
    let volumeSeries = null;
    let smaSeries = null;

    function createPriceSeries() {
        if (typeof chart.addSeries === 'function' && LightweightCharts.CandlestickSeries) {
            candlestickSeries = chart.addSeries(
                LightweightCharts.CandlestickSeries,
                {},
                0
            );

            smaSeries = chart.addSeries(
                LightweightCharts.LineSeries,
                {
                    lineWidth: 2,
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                0
            );
        } else if (typeof chart.addCandlestickSeries === 'function') {
            candlestickSeries = chart.addCandlestickSeries();

            smaSeries = chart.addLineSeries({
                lineWidth: 2,
                lastValueVisible: false,
                priceLineVisible: false
            });
        } else {
            throw new Error('Candlestick series API is not available');
        }
    }

    function createVolumeSeries() {
        if (volumeSeries) {
            return;
        }

        if (typeof chart.addSeries === 'function' && LightweightCharts.HistogramSeries) {
            volumeSeries = chart.addSeries(
                LightweightCharts.HistogramSeries,
                {
                    priceFormat: { type: 'volume' },
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                1
            );
        } else if (typeof chart.addHistogramSeries === 'function') {
            volumeSeries = chart.addHistogramSeries({
                priceFormat: { type: 'volume' },
                lastValueVisible: false,
                priceLineVisible: false
            });
        } else {
            throw new Error('Histogram series API is not available');
        }
    }

    function removeVolumeSeries() {
        if (!volumeSeries) {
            return;
        }

        chart.removeSeries(volumeSeries);
        volumeSeries = null;

        if (typeof chart.panes === 'function') {
            const panes = chart.panes();
            if (panes.length > 1) {
                try {
                    chart.removePane(1);
                } catch (e) {
                    console.warn('Pane removal skipped:', e);
                }
            }
        }
    }

    createPriceSeries();

    if (typeof chart.panes === 'function') {
        const firstPane = chart.panes()[0];
        if (firstPane) {
            firstPane.setHeight(480);
        }
    }

    function ensureVolumePane() {
        if (!showVolumeToggle.checked) {
            removeVolumeSeries();
            return;
        }

        createVolumeSeries();

        if (typeof chart.panes === 'function') {
            const panes = chart.panes();
            if (panes[1]) {
                panes[1].setHeight(140);
            }
        }
    }

    function showLoading() {
        loadingMessage.classList.remove('d-none');
        errorMessage.classList.add('d-none');
    }

    function hideLoading() {
        loadingMessage.classList.add('d-none');
    }

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.remove('d-none');
    }

    function updateInfoLabels(candleData) {
        selectedTickerLabel.textContent = tickerSelect.value;
        selectedIntervalLabel.textContent = intervalSelect.value;

        if (candleData.length > 0) {
            const lastCandle = candleData[candleData.length - 1];
            lastCloseLabel.textContent = Number(lastCandle.close).toLocaleString();
        } else {
            lastCloseLabel.textContent = '-';
        }
    }

    function updateIndicatorLabels() {
        showSmaLabel.textContent = 'Show SMA(' + smaPeriodSelect.value + ')';
    }

    function toVolumeData(candleData) {
        return candleData.map(function (candle) {
            const isBullish = Number(candle.close) >= Number(candle.open);

            return {
                time: candle.time,
                value: Number(candle.volume),
                color: isBullish ? '#26a69a' : '#ef5350'
            };
        });
    }

    function updateInteractionOptions() {
        const scaleEnabled = enableScaleToggle.checked;
        const scrollEnabled = enableScrollToggle.checked;

        chart.applyOptions({
            handleScale: scaleEnabled ? {
                mouseWheel: true,
                pinch: true,
                axisPressedMouseMove: {
                    time: true,
                    price: true
                }
            } : false,
            handleScroll: scrollEnabled ? {
                mouseWheel: true,
                pressedMouseMove: true,
                horzTouchDrag: true,
                vertTouchDrag: true
            } : false
        });
    }

    function loadSma() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;
        const barCount = smaPeriodSelect.value;

        const url = '/api/indicators/sma?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval)
            + '&barCount='
            + encodeURIComponent(barCount);

        return fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load SMA');
                    });
                }
                return response.json();
            });
    }

    function applyData(candleData, smaData) {
        candlestickSeries.setData(candleData);

        if (showVolumeToggle.checked) {
            ensureVolumePane();
            if (volumeSeries) {
                volumeSeries.setData(toVolumeData(candleData));
            }
        } else {
            removeVolumeSeries();
        }

        if (showSmaToggle.checked) {
            smaSeries.setData(smaData);
        } else {
            smaSeries.setData([]);
        }

        chart.timeScale().fitContent();
        updateInfoLabels(candleData);
        errorMessage.classList.add('d-none');
    }

    function loadAllData() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;

        const candlesUrl = '/api/candles?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval);

        showLoading();

        const candlePromise = fetch(candlesUrl)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load candles');
                    });
                }
                return response.json();
            });

        const smaPromise = showSmaToggle.checked
            ? loadSma()
            : Promise.resolve([]);

        Promise.all([candlePromise, smaPromise])
            .then(function (results) {
                const candleData = results[0];
                const smaData = results[1];
                applyData(candleData, smaData);
            })
            .catch(function (error) {
                console.error('Error loading chart data:', error);
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    updateIndicatorLabels();
    updateInteractionOptions();
    ensureVolumePane();
    loadAllData();

    tickerSelect.addEventListener('change', function () {
        loadAllData();
    });

    intervalSelect.addEventListener('change', function () {
        loadAllData();
    });

    smaPeriodSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        loadAllData();
    });

    enableScaleToggle.addEventListener('change', function () {
        updateInteractionOptions();
    });

    enableScrollToggle.addEventListener('change', function () {
        updateInteractionOptions();
    });

    showVolumeToggle.addEventListener('change', function () {
        loadAllData();
    });

    showSmaToggle.addEventListener('change', function () {
        loadAllData();
    });

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});