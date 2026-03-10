document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');
    const tickerSelect = document.getElementById('tickerSelect');
    const intervalSelect = document.getElementById('intervalSelect');
    const loadingMessage = document.getElementById('loadingMessage');
    const errorMessage = document.getElementById('errorMessage');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 500,
        timeScale: {
            timeVisible: true,
            secondsVisible: false
        }
    });

    const candlestickSeries = chart.addSeries(LightweightCharts.CandlestickSeries);

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

    function loadCandles() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;

        const url = '/api/candles?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval);

        showLoading();

        fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (errorBody) {
                        throw new Error(errorBody.message || 'Failed to load candles');
                    });
                }
                return response.json();
            })
            .then(function (data) {
                candlestickSeries.setData(data);
                chart.timeScale().fitContent();
                errorMessage.classList.add('d-none');
            })
            .catch(function (error) {
                console.error('Error loading candles:', error);
                showError(error.message);
            })
            .finally(function () {
                hideLoading();
            });
    }

    loadCandles();

    tickerSelect.addEventListener('change', function () {
        loadCandles();
    });

    intervalSelect.addEventListener('change', function () {
        loadCandles();
    });

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});