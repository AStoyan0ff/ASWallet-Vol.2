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

    const statusRoot = todayCanvas.closest(".admin-chart-status");
    const stage = todayCanvas.closest(".admin-chart-status__stage");
    const plot = todayCanvas.closest(".admin-chart-status__plot");
    const calloutSvg = stage ? stage.querySelector(".admin-chart-callout-svg") : null;
    const calloutsHost = document.getElementById("adminTodayStatusCallouts");

    let hoveredStatusIndex = null;
    let todayChart = null;

    function clearCallouts() {

        if (calloutSvg) {
            calloutSvg.innerHTML = "";
        }

        if (calloutsHost) {
            calloutsHost.innerHTML = "";
        }
    }

    function setHoveredStatus(index) {
        const next = typeof index === "number" && todayCounts[index] > 0
            ? index
            : null;

        if (next === hoveredStatusIndex) {
            return;
        }

        hoveredStatusIndex = next;

        if (todayChart) {
            renderStatusCallouts(todayChart);
        }
    }

    function renderStatusCallouts(chart) {

        if (
            !hasTodayData ||
            hoveredStatusIndex === null ||
            !stage ||
            !plot ||
            !calloutSvg ||
            !calloutsHost) {

            clearCallouts();
            return;
        }

        const meta = chart.getDatasetMeta(0);
        const dataIndex = hoveredStatusIndex;
        const arc = meta && meta.data
            ? meta.data[dataIndex]
            : null;

        if (!arc || !(todayCounts[dataIndex] > 0)) {
            clearCallouts();

            return;
        }

        const label = todayLabels[dataIndex] || "";
        const value = todayCounts[dataIndex] || 0;
        const expected = label + ": " + value;

        const props = arc.getProps(["x", "y", "startAngle", "endAngle", "outerRadius"], true);

        const midAngle = (props.startAngle + props.endAngle) / 2;
        const onLeft = Math.cos(midAngle) < 0;
        const sideClass = onLeft
            ? "is-left"
            : "is-right";

        let box = calloutsHost.querySelector(".admin-chart-callout-box");

        if (!box ||
            box.getAttribute("data-index") !== String(dataIndex) ||
            box.textContent !== expected) {

            calloutsHost.innerHTML =
                '<div ' +
                    'class="admin-chart-callout-box ' + sideClass + '" data-index="' + dataIndex + '">' + expected +
                "</div>";

            box = calloutsHost.querySelector(".admin-chart-callout-box");

        } else {

            box.classList.remove("is-left", "is-right");
            box.classList.add(sideClass);
        }

        const stageRect = stage.getBoundingClientRect();
        const plotRect = plot.getBoundingClientRect();
        const stageWidth = stage.clientWidth;
        const stageHeight = stage.clientHeight;
        const centerX = plotRect.left - stageRect.left + props.x;
        const centerY = plotRect.top - stageRect.top + props.y;
        const lineLength = 16;
        const startX = centerX + Math.cos(midAngle) * props.outerRadius;
        const startY = centerY + Math.sin(midAngle) * props.outerRadius;

        let endX = centerX + Math.cos(midAngle) * (props.outerRadius + lineLength);
        let endY = centerY + Math.sin(midAngle) * (props.outerRadius + lineLength);

        endX = Math.max(8, Math.min(stageWidth - 8, endX));
        endY = Math.max(10, Math.min(stageHeight - 10, endY));

        calloutSvg.setAttribute("viewBox", "0 0 " + stageWidth + " " + stageHeight);
        calloutSvg.setAttribute("width", String(stageWidth));
        calloutSvg.setAttribute("height", String(stageHeight));

        calloutSvg.style.width = stageWidth + "px";
        calloutSvg.style.height = stageHeight + "px";

        if (!box) {

            calloutSvg.innerHTML = "";
            return;
        }

        const gap = 4;

        box.style.left = Math.round(endX + (onLeft ? -gap : gap)) + "px";
        box.style.top = Math.round(endY) + "px";

        calloutSvg.innerHTML =
            '<line class="admin-chart-callout-path" x1="' +
            startX.toFixed(1) +
            '" y1="' +
            startY.toFixed(1) +
            '" x2="' +
            endX.toFixed(1) +
            '" y2="' +
            endY.toFixed(1) +
            '" />';
    }

    todayChart = new Chart(todayCanvas, {

        type: "doughnut",
        data: {

            labels: todayLabels,
            datasets: [{

                data: todayValues,
                backgroundColor: todayColors,
                borderWidth: 0,
                hoverOffset: hasTodayData
                    ? 2
                    : 0
            }]
        },
        options: {

            responsive: true,
            maintainAspectRatio: false,
            cutout: "58%",

            layout: {
                padding: 2
            },
            onHover: function (event, elements) {

                if (!hasTodayData) {
                    setHoveredStatus(null);

                    if (event && event.native && event.native.target) {
                        event.native.target.style.cursor = "default";
                    }

                    return;
                }

                if (elements && elements.length) {
                    setHoveredStatus(elements[0].index);

                    if (event && event.native && event.native.target) {
                        event.native.target.style.cursor = "pointer";
                    }

                } else {
                    setHoveredStatus(null);

                    if (event && event.native && event.native.target) {
                        event.native.target.style.cursor = "default";
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },

                tooltip: {
                    enabled: false
                }
            }
        },
        plugins: [{

            id: "adminStatusHoverCallouts",
            afterDraw: function (chart) {

                if (hoveredStatusIndex !== null) {
                    renderStatusCallouts(chart);
                }
            }
        }]
    });

    todayCanvas.addEventListener("mouseleave", function () {
        setHoveredStatus(null);
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
