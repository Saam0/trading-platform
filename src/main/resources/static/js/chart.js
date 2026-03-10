document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 500
    });

    const candlestickSeries = chart.addSeries(LightweightCharts.CandlestickSeries);

    fetch('/api/candles')
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

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});