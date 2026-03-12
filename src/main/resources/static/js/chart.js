document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');
    const tickerSelect = document.getElementById('tickerSelect');
    const intervalSelect = document.getElementById('intervalSelect');
    const enableScaleToggle = document.getElementById('enableScaleToggle');
    const enableScrollToggle = document.getElementById('enableScrollToggle');
    const showVolumeToggle = document.getElementById('showVolumeToggle');

    const showSmaToggle = document.getElementById('showSmaToggle');
    const showEmaToggle = document.getElementById('showEmaToggle');
    const showCeToggle = document.getElementById('showCeToggle');

    const smaPeriodSelect = document.getElementById('smaPeriodSelect');
    const emaPeriodSelect = document.getElementById('emaPeriodSelect');
    const ceLengthSelect = document.getElementById('ceLengthSelect');
    const ceMultiplierInput = document.getElementById('ceMultiplierInput');
    const ceUseCloseToggle = document.getElementById('ceUseCloseToggle');

    const loadingMessage = document.getElementById('loadingMessage');
    const errorMessage = document.getElementById('errorMessage');

    const selectedTickerLabel = document.getElementById('selectedTickerLabel');
    const selectedIntervalLabel = document.getElementById('selectedIntervalLabel');
    const lastCloseLabel = document.getElementById('lastCloseLabel');

    const showSmaLabel = document.getElementById('showSmaLabel');
    const showEmaLabel = document.getElementById('showEmaLabel');
    const showCeLabel = document.getElementById('showCeLabel');

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
    let emaSeries = null;
    let ceLongSeries = null;
    let ceShortSeries = null;
    let ceMarkersPrimitive = null;

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
                    color: '#2962FF',
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                0
            );

            emaSeries = chart.addSeries(
                LightweightCharts.LineSeries,
                {
                    lineWidth: 2,
                    color: '#FF6D00',
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                0
            );

            ceLongSeries = chart.addSeries(
                LightweightCharts.LineSeries,
                {
                    lineWidth: 2,
                    color: '#2e7d32',
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                0
            );

            ceShortSeries = chart.addSeries(
                LightweightCharts.LineSeries,
                {
                    lineWidth: 2,
                    color: '#e53935',
                    lastValueVisible: false,
                    priceLineVisible: false
                },
                0
            );
        } else if (typeof chart.addCandlestickSeries === 'function') {
            candlestickSeries = chart.addCandlestickSeries();

            smaSeries = chart.addLineSeries({
                lineWidth: 2,
                color: '#2962FF',
                lastValueVisible: false,
                priceLineVisible: false
            });

            emaSeries = chart.addLineSeries({
                lineWidth: 2,
                color: '#FF6D00',
                lastValueVisible: false,
                priceLineVisible: false
            });

            ceLongSeries = chart.addLineSeries({
                lineWidth: 2,
                color: '#2e7d32',
                lastValueVisible: false,
                priceLineVisible: false
            });

            ceShortSeries = chart.addLineSeries({
                lineWidth: 2,
                color: '#e53935',
                lastValueVisible: false,
                priceLineVisible: false
            });
        } else {
            throw new Error('Candlestick series API is not available');
        }

        setupMarkersPrimitive();
    }

    function setupMarkersPrimitive() {
        if (typeof candlestickSeries.setMarkers === 'function') {
            return;
        }

        if (typeof LightweightCharts.createSeriesMarkers === 'function') {
            ceMarkersPrimitive = LightweightCharts.createSeriesMarkers(candlestickSeries, []);
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
        showEmaLabel.textContent = 'Show EMA(' + emaPeriodSelect.value + ')';
        showCeLabel.textContent =
            'Show Chandelier Exit (' + ceLengthSelect.value + ', x' + ceMultiplierInput.value + ')';
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

    function toCeLongData(ceData) {
        return ceData.map(function (point) {
            if (point.direction !== 1 || point.longStop === null || point.longStop === undefined) {
                return { time: point.time };
            }

            return {
                time: point.time,
                value: Number(point.longStop)
            };
        });
    }

    function toCeShortData(ceData) {
        return ceData.map(function (point) {
            if (point.direction !== -1 || point.shortStop === null || point.shortStop === undefined) {
                return { time: point.time };
            }

            return {
                time: point.time,
                value: Number(point.shortStop)
            };
        });
    }

    function toCeMarkers(ceData) {
        const markers = [];

        ceData.forEach(function (point) {
            if (point.buySignal === true) {
                markers.push({
                    time: point.time,
                    position: 'belowBar',
                    color: '#4caf50',
                    shape: 'arrowUp',
                    text: 'Buy'
                });
            }

            if (point.sellSignal === true) {
                markers.push({
                    time: point.time,
                    position: 'aboveBar',
                    color: '#ef5350',
                    shape: 'arrowDown',
                    text: 'Sell'
                });
            }
        });

        return markers;
    }

    function applyMarkers(markers) {
        if (typeof candlestickSeries.setMarkers === 'function') {
            candlestickSeries.setMarkers(markers);
            return;
        }

        if (ceMarkersPrimitive && typeof ceMarkersPrimitive.setMarkers === 'function') {
            ceMarkersPrimitive.setMarkers(markers);
        }
    }

    function clearMarkers() {
        applyMarkers([]);
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

    function loadCandles() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;

        const url = '/api/candles?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval);

        return fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load candles');
                    });
                }
                return response.json();
            });
    }

    function loadSma() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;
        const period = smaPeriodSelect.value;

        const url = '/api/indicators/sma?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval)
            + '&period='
            + encodeURIComponent(period);

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

    function loadEma() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;
        const period = emaPeriodSelect.value;

        const url = '/api/indicators/ema?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval)
            + '&period='
            + encodeURIComponent(period);

        return fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load EMA');
                    });
                }
                return response.json();
            });
    }

    function loadChandelierExit() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;
        const length = ceLengthSelect.value;
        const multiplier = ceMultiplierInput.value;
        const useClose = ceUseCloseToggle.checked;

        const url = '/api/indicators/chandelier-exit?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval)
            + '&length='
            + encodeURIComponent(length)
            + '&multiplier='
            + encodeURIComponent(multiplier)
            + '&useClose='
            + encodeURIComponent(useClose);

        return fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load Chandelier Exit');
                    });
                }
                return response.json();
            });
    }

    function applyData(candleData, smaData, emaData, ceData) {
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

        if (showEmaToggle.checked) {
            emaSeries.setData(emaData);
        } else {
            emaSeries.setData([]);
        }

        if (showCeToggle.checked) {
            ceLongSeries.setData(toCeLongData(ceData));
            ceShortSeries.setData(toCeShortData(ceData));
            applyMarkers(toCeMarkers(ceData));
        } else {
            ceLongSeries.setData([]);
            ceShortSeries.setData([]);
            clearMarkers();
        }

        chart.timeScale().fitContent();
        updateInfoLabels(candleData);
        errorMessage.classList.add('d-none');
    }

    function loadAllData() {
        showLoading();

        const candlePromise = loadCandles();

        const smaPromise = showSmaToggle.checked
            ? loadSma()
            : Promise.resolve([]);

        const emaPromise = showEmaToggle.checked
            ? loadEma()
            : Promise.resolve([]);

        const cePromise = showCeToggle.checked
            ? loadChandelierExit()
            : Promise.resolve([]);

        Promise.all([candlePromise, smaPromise, emaPromise, cePromise])
            .then(function (results) {
                const candleData = results[0];
                const smaData = results[1];
                const emaData = results[2];
                const ceData = results[3];

                applyData(candleData, smaData, emaData, ceData);
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

    emaPeriodSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        loadAllData();
    });

    ceLengthSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        loadAllData();
    });

    ceMultiplierInput.addEventListener('change', function () {
        updateIndicatorLabels();
        loadAllData();
    });

    ceUseCloseToggle.addEventListener('change', function () {
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

    showEmaToggle.addEventListener('change', function () {
        loadAllData();
    });

    showCeToggle.addEventListener('change', function () {
        updateIndicatorLabels();
        loadAllData();
    });

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});