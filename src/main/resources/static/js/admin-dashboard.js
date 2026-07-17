document.addEventListener("DOMContentLoaded", () => {
    const data = window.adminDashboardChartData;
    const todayCanvas = document.getElementById("adminTodayStatusChart");
    const weekCanvas = document.getElementById("adminWeekTransfersChart");

    if (!data || typeof Chart === "undefined" || !todayCanvas || !weekCanvas) {
        return;
    }

    const textColor = "rgba(255, 244, 225, 0.88)";
    const gridColor = "rgba(255, 236, 190, 0.18)";

    const todayCounts = (data.todayCounts || []).map(Number);
    const weekCounts = (data.weekCounts || []).map(Number);
    const hasTodayData = todayCounts.some((value) => value > 0);

    new Chart(todayCanvas, {
        type: "doughnut",
        data: {
            labels: data.todayLabels || [],
            datasets: [{
                data: hasTodayData ? todayCounts : [1],
                backgroundColor: hasTodayData
                    ? ["#ffb74d", "#ffd54f", "#81c784", "#e57373", "#b0bec5"]
                    : ["rgba(255, 255, 255, 0.18)"],
                borderWidth: 0,
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: "right",
                    labels: {
                        color: textColor,
                        boxWidth: 10,
                        font: { size: 11 }
                    }
                },
                tooltip: {
                    enabled: hasTodayData
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
