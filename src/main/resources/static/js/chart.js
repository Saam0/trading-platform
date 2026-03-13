document.addEventListener('DOMContentLoaded', function () {
    console.log('chart.js VERSION: historical-lazy-loading-v2');

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

    const INITIAL_LIMIT = 30;
    const HISTORY_PAGE_SIZE = 30;
    const LEFT_EDGE_THRESHOLD = 5;

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

    let ceLongSegmentSeries = [];
    let ceShortSegmentSeries = [];
    let ceMarkersPrimitive = null;

    let allCandles = [];
    let isLoadingOlder = false;
    let hasMoreHistory = true;

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

    function addLineSeries(options) {
        if (typeof chart.addSeries === 'function' && LightweightCharts.LineSeries) {
            return chart.addSeries(LightweightCharts.LineSeries, options, 0);
        }

        if (typeof chart.addLineSeries === 'function') {
            return chart.addLineSeries(options);
        }

        throw new Error('Line series API is not available');
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

    function clearCeSegmentSeries() {
        ceLongSegmentSeries.forEach(function (series) {
            chart.removeSeries(series);
        });

        ceShortSegmentSeries.forEach(function (series) {
            chart.removeSeries(series);
        });

        ceLongSegmentSeries = [];
        ceShortSegmentSeries = [];
    }

    function buildCeSegments(ceData, directionValue, stopFieldName) {
        const segments = [];
        let currentSegment = [];

        ceData.forEach(function (point) {
            const stopValue = point[stopFieldName];
            const isActive =
                point.direction === directionValue &&
                stopValue !== null &&
                stopValue !== undefined;

            if (isActive) {
                currentSegment.push({
                    time: point.time,
                    value: Number(stopValue)
                });
            } else if (currentSegment.length > 0) {
                segments.push(currentSegment);
                currentSegment = [];
            }
        });

        if (currentSegment.length > 0) {
            segments.push(currentSegment);
        }

        return segments;
    }

    function renderCeSegments(ceData) {
        clearCeSegmentSeries();

        const longSegments = buildCeSegments(ceData, 1, 'longStop');
        const shortSegments = buildCeSegments(ceData, -1, 'shortStop');

        longSegments.forEach(function (segmentData) {
            const series = addLineSeries({
                lineWidth: 2,
                color: '#2e7d32',
                lastValueVisible: false,
                priceLineVisible: false
            });

            series.setData(segmentData);
            ceLongSegmentSeries.push(series);
        });

        shortSegments.forEach(function (segmentData) {
            const series = addLineSeries({
                lineWidth: 2,
                color: '#e53935',
                lastValueVisible: false,
                priceLineVisible: false
            });

            series.setData(segmentData);
            ceShortSegmentSeries.push(series);
        });
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

    function showLoading(message) {
        loadingMessage.textContent = message || 'Loading...';
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

    function captureChartViewState() {
        return {
            logicalRange: chart.timeScale().getVisibleLogicalRange(),
            scrollPosition: chart.timeScale().scrollPosition()
        };
    }

    function restoreChartViewState(viewState) {
        if (!viewState) {
            return;
        }

        requestAnimationFrame(function () {
            if (viewState.logicalRange) {
                chart.timeScale().setVisibleLogicalRange(viewState.logicalRange);
                return;
            }

            if (typeof viewState.scrollPosition === 'number') {
                chart.timeScale().scrollToPosition(viewState.scrollPosition, false);
            }
        });
    }

    function candleTimeToMillis(time) {
        if (typeof time === 'number') {
            return time * 1000;
        }

        if (typeof time === 'string' && time.length === 10) {
            return Date.parse(time + 'T00:00:00Z');
        }

        return Date.parse(time);
    }

    function getOldestCandleTimeMillis() {
        if (allCandles.length === 0) {
            return null;
        }

        return candleTimeToMillis(allCandles[0].time);
    }

    function mergeCandles(existingCandles, newOlderCandles) {
        const merged = newOlderCandles.concat(existingCandles);
        const uniqueByTime = new Map();

        merged.forEach(function (candle) {
            uniqueByTime.set(String(candle.time), candle);
        });

        return Array.from(uniqueByTime.values()).sort(function (a, b) {
            return candleTimeToMillis(a.time) - candleTimeToMillis(b.time);
        });
    }

    function buildCandlesUrl(limit, to) {
        let url = '/api/candles?ticker='
            + encodeURIComponent(tickerSelect.value)
            + '&interval='
            + encodeURIComponent(intervalSelect.value)
            + '&limit='
            + encodeURIComponent(limit);

        if (to !== null && to !== undefined) {
            url += '&to=' + encodeURIComponent(to);
        }

        return url;
    }

    function buildSmaUrl(limit, to) {
        let url = '/api/indicators/sma?ticker='
            + encodeURIComponent(tickerSelect.value)
            + '&interval='
            + encodeURIComponent(intervalSelect.value)
            + '&period='
            + encodeURIComponent(smaPeriodSelect.value)
            + '&limit='
            + encodeURIComponent(limit);

        if (to !== null && to !== undefined) {
            url += '&to=' + encodeURIComponent(to);
        }

        return url;
    }

    function buildEmaUrl(limit, to) {
        let url = '/api/indicators/ema?ticker='
            + encodeURIComponent(tickerSelect.value)
            + '&interval='
            + encodeURIComponent(intervalSelect.value)
            + '&period='
            + encodeURIComponent(emaPeriodSelect.value)
            + '&limit='
            + encodeURIComponent(limit);

        if (to !== null && to !== undefined) {
            url += '&to=' + encodeURIComponent(to);
        }

        return url;
    }

    function buildCeUrl(limit, to) {
        let url = '/api/indicators/chandelier-exit?ticker='
            + encodeURIComponent(tickerSelect.value)
            + '&interval='
            + encodeURIComponent(intervalSelect.value)
            + '&length='
            + encodeURIComponent(ceLengthSelect.value)
            + '&multiplier='
            + encodeURIComponent(ceMultiplierInput.value)
            + '&useClose='
            + encodeURIComponent(ceUseCloseToggle.checked)
            + '&limit='
            + encodeURIComponent(limit);

        if (to !== null && to !== undefined) {
            url += '&to=' + encodeURIComponent(to);
        }

        return url;
    }

    function fetchJson(url, defaultMessage) {
        return fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json()
                        .then(function (errorBody) {
                            throw new Error(errorBody.message || defaultMessage);
                        })
                        .catch(function () {
                            throw new Error(defaultMessage);
                        });
                }

                return response.json();
            });
    }

    function loadCandles(limit, to) {
        return fetchJson(
            buildCandlesUrl(limit, to),
            'Failed to load candles'
        );
    }

    function loadSma(limit, to) {
        return fetchJson(
            buildSmaUrl(limit, to),
            'Failed to load SMA'
        );
    }

    function loadEma(limit, to) {
        return fetchJson(
            buildEmaUrl(limit, to),
            'Failed to load EMA'
        );
    }

    function loadChandelierExit(limit, to) {
        return fetchJson(
            buildCeUrl(limit, to),
            'Failed to load Chandelier Exit'
        );
    }

    function applyData(candleData, smaData, emaData, ceData, options) {
        const resetView = options && options.resetView === true;
        const preserveLogicalRange = options && options.preserveLogicalRange === true;
        const viewState = options ? options.viewState : null;

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
            renderCeSegments(ceData);
            applyMarkers(toCeMarkers(ceData));
        } else {
            clearCeSegmentSeries();
            clearMarkers();
        }

        if (resetView) {
            chart.timeScale().fitContent();
        } else if (!preserveLogicalRange) {
            restoreChartViewState(viewState);
        }

        updateInfoLabels(candleData);
        errorMessage.classList.add('d-none');
    }

    function buildIndicatorPromises(limit, to) {
        const smaPromise = showSmaToggle.checked
            ? loadSma(limit, to)
            : Promise.resolve([]);

        const emaPromise = showEmaToggle.checked
            ? loadEma(limit, to)
            : Promise.resolve([]);

        const cePromise = showCeToggle.checked
            ? loadChandelierExit(limit, to)
            : Promise.resolve([]);

        return [smaPromise, emaPromise, cePromise];
    }

    function loadInitialData(options) {
        const resetView = options && options.resetView === true;
        const viewState = resetView ? null : captureChartViewState();

        allCandles = [];
        isLoadingOlder = false;
        hasMoreHistory = true;

        showLoading('Loading candles...');

        const candlePromise = loadCandles(INITIAL_LIMIT, null);
        const indicatorPromises = buildIndicatorPromises(INITIAL_LIMIT, null);

        Promise.all([candlePromise].concat(indicatorPromises))
            .then(function (results) {
                const candleData = results[0];
                const smaData = results[1];
                const emaData = results[2];
                const ceData = results[3];

                allCandles = candleData;

                if (candleData.length < INITIAL_LIMIT) {
                    hasMoreHistory = false;
                }

                applyData(candleData, smaData, emaData, ceData, {
                    resetView: resetView,
                    viewState: viewState
                });
            })
            .catch(function (error) {
                console.error('Error loading chart data:', error);
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    function reloadCurrentWindow(options) {
        const resetView = options && options.resetView === true;
        const viewState = resetView ? null : captureChartViewState();
        const currentLimit = allCandles.length > 0 ? allCandles.length : INITIAL_LIMIT;

        showLoading('Reloading indicators...');

        const candlePromise = allCandles.length > 0
            ? Promise.resolve(allCandles)
            : loadCandles(currentLimit, null);

        const indicatorPromises = buildIndicatorPromises(currentLimit, null);

        Promise.all([candlePromise].concat(indicatorPromises))
            .then(function (results) {
                const candleData = results[0];
                const smaData = results[1];
                const emaData = results[2];
                const ceData = results[3];

                allCandles = candleData;

                applyData(candleData, smaData, emaData, ceData, {
                    resetView: resetView,
                    viewState: viewState
                });
            })
            .catch(function (error) {
                console.error('Error reloading chart window:', error);
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    function loadOlderHistory() {
        if (isLoadingOlder || !hasMoreHistory || allCandles.length === 0) {
            return;
        }

        isLoadingOlder = true;

        const oldestTime = getOldestCandleTimeMillis();

        if (oldestTime === null) {
            isLoadingOlder = false;
            return;
        }

        const requestTo = oldestTime - 1;
        const previousRange = chart.timeScale().getVisibleLogicalRange();

        loadCandles(HISTORY_PAGE_SIZE, requestTo)
            .then(function (olderCandles) {
                if (!olderCandles || olderCandles.length === 0) {
                    hasMoreHistory = false;
                    return Promise.resolve([]);
                }

                const oldLength = allCandles.length;
                allCandles = mergeCandles(allCandles, olderCandles);
                const addedCount = allCandles.length - oldLength;

                if (olderCandles.length < HISTORY_PAGE_SIZE) {
                    hasMoreHistory = false;
                }

                const fullLimit = allCandles.length;
                const indicatorPromises = buildIndicatorPromises(fullLimit, null);

                return Promise.all(indicatorPromises)
                    .then(function (indicatorResults) {
                        const smaData = indicatorResults[0];
                        const emaData = indicatorResults[1];
                        const ceData = indicatorResults[2];

                        applyData(allCandles, smaData, emaData, ceData, {
                            resetView: false,
                            preserveLogicalRange: true
                        });

                        if (previousRange && addedCount > 0) {
                            requestAnimationFrame(function () {
                                chart.timeScale().setVisibleLogicalRange({
                                    from: previousRange.from + addedCount,
                                    to: previousRange.to + addedCount
                                });
                            });
                        }
                    });
            })
            .catch(function (error) {
                console.error('Error loading older history:', error);
                showError(error.message);
            })
            .finally(function () {
                isLoadingOlder = false;
            });
    }

    function handleVisibleRangeChange(logicalRange) {
        if (!logicalRange) {
            return;
        }

        if (logicalRange.from <= LEFT_EDGE_THRESHOLD) {
            loadOlderHistory();
        }
    }

    updateIndicatorLabels();
    updateInteractionOptions();
    ensureVolumePane();
    loadInitialData({ resetView: true });

    chart.timeScale().subscribeVisibleLogicalRangeChange(handleVisibleRangeChange);

    tickerSelect.addEventListener('change', function () {
        loadInitialData({ resetView: true });
    });

    intervalSelect.addEventListener('change', function () {
        loadInitialData({ resetView: true });
    });

    smaPeriodSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    emaPeriodSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    ceLengthSelect.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    ceMultiplierInput.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    ceUseCloseToggle.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    enableScaleToggle.addEventListener('change', function () {
        updateInteractionOptions();
    });

    enableScrollToggle.addEventListener('change', function () {
        updateInteractionOptions();
    });

    showVolumeToggle.addEventListener('change', function () {
        reloadCurrentWindow({ resetView: false });
    });

    showSmaToggle.addEventListener('change', function () {
        reloadCurrentWindow({ resetView: false });
    });

    showEmaToggle.addEventListener('change', function () {
        reloadCurrentWindow({ resetView: false });
    });

    showCeToggle.addEventListener('change', function () {
        updateIndicatorLabels();
        reloadCurrentWindow({ resetView: false });
    });

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});