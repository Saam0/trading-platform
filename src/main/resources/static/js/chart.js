document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');
    const tickerSelect = document.getElementById('tickerSelect');
    const intervalSelect = document.getElementById('intervalSelect');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 500,
        timeScale: {
            timeVisible: true,
            secondsVisible: false
        }
    });

    const candlestickSeries = chart.addSeries(LightweightCharts.CandlestickSeries);

    function loadCandles() {
        const ticker = tickerSelect.value;
        const interval = intervalSelect.value;

        const url = '/api/candles?ticker='
            + encodeURIComponent(ticker)
            + '&interval='
            + encodeURIComponent(interval);

        fetch(url)
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                candlestickSeries.setData(data);
                chart.timeScale().fitContent();
            })
            .catch(function (error) {
                console.error('Error loading candles:', error);
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