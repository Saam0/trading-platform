document.addEventListener("DOMContentLoaded", () => {

    const runBtn = document.getElementById("runBacktestBtn");

    if (!runBtn) return;

    runBtn.addEventListener("click", runBacktest);

});


function runBacktest() {

    const ticker = document.getElementById("tickerSelect").value;
    const interval = document.getElementById("intervalSelect").value;

    const limit = Number(document.getElementById("btLimit").value);
    const length = Number(document.getElementById("btLength").value);
    const multiplier = Number(document.getElementById("btMult").value);
    const capital = Number(document.getElementById("btCapital").value);

    const fromValue = document.getElementById("btFrom").value;
    const toValue = document.getElementById("btTo").value;

    let from = null;
    let to = null;

    if (fromValue) {
        from = new Date(fromValue).getTime();
    }

    if (toValue) {
        to = new Date(toValue).getTime();
    }

    const request = {
        ticker,
        interval,
        limit,
        length,
        multiplier,
        initialCapital: capital
    };

    if (from) request.from = from;
    if (to) request.to = to;

    fetch("/api/backtest/chandelier-exit", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(request)
    })
        .then(r => r.json())
        .then(renderBacktest)
        .catch(err => {
            console.error("Backtest error", err);
        });

}


function renderBacktest(result) {

    document.getElementById("btInitial").textContent =
        "$" + result.initialCapital.toFixed(2);

    document.getElementById("btFinal").textContent =
        "$" + result.finalCapital.toFixed(2);

    document.getElementById("btProfit").textContent =
        "$" + result.netProfit.toFixed(2);

    document.getElementById("btWinRate").textContent =
        result.winRate.toFixed(2) + "%";

}