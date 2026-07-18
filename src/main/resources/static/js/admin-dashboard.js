document.addEventListener("DOMContentLoaded", () => {

    const data = window.adminDashboardChartData;
    const todayCanvas = document.getElementById("adminTodayStatusChart");
    const weekCanvas = document.getElementById("adminWeekTransfersChart");

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

    const todayColors = todayLabels.map((_, index) => statusColors[index % statusColors.length]);
    const todayValues = hasTodayData
        ? todayCounts
        : todayLabels.map(() => 1);

    new Chart(todayCanvas, {
        type: "doughnut",

        data: {

            labels: todayLabels,
            datasets: [{
                data: todayValues,
                backgroundColor: todayColors,
                borderWidth: 0,
                hoverOffset: hasTodayData ? 4 : 0
            }]
        },
        options: {

            responsive: true,
            maintainAspectRatio: false,
            cutout: "58%",

            plugins: {
                legend: {
                    position: "right",
                    onClick: null,
                    labels: {
                        color: "#fff4e1",
                        boxWidth: 12,
                        boxHeight: 12,
                        padding: 8,
                        font: { size: 11 },
                        generateLabels: function (chart) {
                            const labels = chart.data.labels || [];
                            return labels.map(function (label, index) {
                                const color = statusColors[index % statusColors.length];
                                return {
                                    text: label,
                                    fillStyle: color,
                                    strokeStyle: color,
                                    fontColor: "#fff4e1",
                                    lineWidth: 0,
                                    hidden: false,
                                    index: index
                                };
                            });
                        }
                    }
                },
                tooltip: {
                    enabled: hasTodayData,
                    callbacks: {
                        label: function (context) {
                            const label = context.label || "";
                            const value = todayCounts[context.dataIndex] || 0;
                            return label + ": " + value;
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
