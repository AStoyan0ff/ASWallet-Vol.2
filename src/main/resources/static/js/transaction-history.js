document.addEventListener("DOMContentLoaded", () => {
    const panel = document.querySelector("[data-tx-auto-refresh='true']");

    if (!panel) {
        return;
    }

    const seconds = Number.parseInt(panel.dataset.txRefreshSeconds, 10);
    const refreshMs = (Number.isFinite(seconds) && seconds > 0 ? seconds : 12) * 1000;

    window.setInterval(() => {
            window.location.reload();
        },
        refreshMs);
});
