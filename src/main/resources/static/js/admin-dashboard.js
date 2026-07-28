document.addEventListener("DOMContentLoaded", () => {

    const data = window.adminDashboardChartData;
    const todayCanvas = document.getElementById("adminTodayStatusChart");
    const weekCanvas = document.getElementById("adminWeekTransfersChart");
    const todayLegend = document.getElementById("adminTodayStatusLegend");

    if (!data || typeof Chart === "undefined" || !todayCanvas || !weekCanvas) {
        return;
    }

    const textColor = "rgba(255, 244, 225, 0.88)";
    const gridColor = "rgba(255, 236, 190, 0.18)";

    const statusColors = ["#ffb74d", "#e53935", "#66bb6a", "#ff7043", "#90a4ae"];

    const todayLabels = data.todayLabels || [];
    const todayCounts = (data.todayCounts || []).map(Number);
    const weekCounts = (data.weekCounts || []).map(Number);
    const hasTodayData = todayCounts.some((value) => value > 0);

    const todayColors = todayLabels.map((_, index) =>
        statusColors[index % statusColors.length]);

    const todayValues = hasTodayData
        ? todayCounts
        : todayLabels.map(() => 1);

    if (todayLegend) {

        todayLegend.innerHTML = todayLabels.map(function (label, index) {
            const color = statusColors[index % statusColors.length];

            return (
                '<li class="admin-chart-status__legend-item">' +
                '<span class="admin-chart-status__swatch" style="background:' + color + '"></span>' +
                '<span class="admin-chart-status__legend-label">' + label + "</span>" +
                "</li>"
            );

        }).join("");
    }

    const calloutSvg = todayCanvas.closest(".admin-chart-status__stage")
        ? todayCanvas.closest(".admin-chart-status__stage").querySelector(".admin-chart-callout-svg")
        : null;
    const calloutsHost = document.getElementById("adminTodayStatusCallouts");

    if (calloutSvg) {
        calloutSvg.innerHTML = "";
        calloutSvg.style.display = "none";
    }

    if (calloutsHost) {
        calloutsHost.innerHTML = "";
        calloutsHost.style.display = "none";
    }

    new Chart(todayCanvas, {

        type: "doughnut",
        data: {

            labels: todayLabels,
            datasets: [{

                data: todayValues,
                backgroundColor: todayColors,
                borderColor: "rgba(255, 255, 255, 0.55)",
                borderWidth: hasTodayData ? 2 : 0,
                hoverOffset: hasTodayData ? 6 : 0
            }]
        },
        options: {

            responsive: true,
            maintainAspectRatio: false,
            cutout: "58%",
            layout: {
                padding: 4
            },
            onHover: function (event, elements) {
                if (!event || !event.native || !event.native.target) {
                    return;
                }

                event.native.target.style.cursor =
                    hasTodayData && elements && elements.length
                        ? "pointer"
                        : "default";
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    enabled: hasTodayData,
                    callbacks: {
                        label: function (context) {
                            const value = Number(context.raw || 0);
                            return " " + context.label + ": " + value;
                        }
                    }
                }
            }
        }
    });

    new Chart(weekCanvas, {

        type: "bar",
        data: {

            labels: data.weekLabels || [],
            datasets: [{

                label: "Transfers",
                data: weekCounts,
                backgroundColor: "rgba(212, 175, 55, 0.78)",
                borderColor: "rgba(255, 224, 130, 0.9)",
                borderWidth: 1,
                borderRadius: 6,
                maxBarThickness: 28
            }]
        },

        options: {

            responsive: true,
            maintainAspectRatio: false,
            plugins: {

                legend: { display: false }
            },

            scales: {

                x: {
                    ticks: { color: textColor, font: { size: 10 } },
                    grid: { display: false }
                },

                y: {
                    beginAtZero: true,
                    ticks: {

                        color: textColor,
                        font: { size: 10 },
                        precision: 0
                    },

                    grid: { color: gridColor }
                }
            }
        }
    });
});
