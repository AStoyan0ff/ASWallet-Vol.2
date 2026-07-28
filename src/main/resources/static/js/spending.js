(() => {
    const canvas = document.getElementById("spendingChart");
    if (!canvas || typeof Chart === "undefined") {
        return;
    }

    const readJson = (id) => {
        const node = document.getElementById(id);
        if (!node) {
            return [];
        }
        try {
            return JSON.parse(node.textContent || "[]");
        } catch (error) {
            return [];
        }
    };

    const labels = readJson("spending-chart-labels");
    const amounts = readJson("spending-chart-amounts").map(Number);
    const colors = readJson("spending-chart-colors");

    if (!labels.length || !amounts.length) {
        return;
    }

    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels,
            datasets: [{
                data: amounts,
                backgroundColor: colors,
                borderColor: "rgba(255, 255, 255, 0.55)",
                borderWidth: 2,
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            cutout: "58%",
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label(context) {
                            const value = Number(context.raw || 0).toFixed(2);
                            return ` ${context.label}: €${value}`;
                        }
                    }
                }
            }
        }
    });
})();
