document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');
    const tickerSelect = document.getElementById('tickerSelect');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 500
    });

    const candlestickSeries = chart.addSeries(LightweightCharts.CandlestickSeries);

    function loadCandles(ticker) {
        fetch('/api/candles?ticker=' + encodeURIComponent(ticker))
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

    loadCandles(tickerSelect.value);

    tickerSelect.addEventListener('change', function () {
        loadCandles(tickerSelect.value);
    });

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});