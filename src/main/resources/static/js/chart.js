document.addEventListener('DOMContentLoaded', function () {
    const chartContainer = document.getElementById('chart');

    const chart = LightweightCharts.createChart(chartContainer, {
        width: chartContainer.clientWidth,
        height: 500
    });

    const candlestickSeries = chart.addSeries(LightweightCharts.CandlestickSeries);

    const candleData = [
        { time: '2026-03-01', open: 84200, high: 85150, low: 83850, close: 84820 },
        { time: '2026-03-02', open: 84820, high: 85600, low: 84400, close: 85310 },
        { time: '2026-03-03', open: 85310, high: 86040, low: 85010, close: 85790 },
        { time: '2026-03-04', open: 85790, high: 86120, low: 84550, close: 84980 },
        { time: '2026-03-05', open: 84980, high: 85430, low: 84220, close: 84590 },
        { time: '2026-03-06', open: 84590, high: 85210, low: 84000, close: 85050 },
        { time: '2026-03-07', open: 85050, high: 86400, low: 84880, close: 86120 },
        { time: '2026-03-08', open: 86120, high: 86890, low: 85610, close: 86670 },
        { time: '2026-03-09', open: 86670, high: 87250, low: 86020, close: 86340 },
        { time: '2026-03-10', open: 86340, high: 87010, low: 85830, close: 86880 }
    ];

    candlestickSeries.setData(candleData);

    chart.timeScale().fitContent();

    window.addEventListener('resize', function () {
        chart.applyOptions({
            width: chartContainer.clientWidth
        });
    });
});