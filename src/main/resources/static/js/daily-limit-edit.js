document.addEventListener("DOMContentLoaded", () => {

    const stepper = document.querySelector(".daily-limit-stepper");
    const input = document.getElementById("dailyWithdrawLimit");
    const valueEl = document.getElementById("dailyWithdrawLimitValue");
    const upBtn = stepper?.querySelector(".daily-limit-stepper-btn--up");
    const downBtn = stepper?.querySelector(".daily-limit-stepper-btn--down");

    if (!stepper || !input || !valueEl || !upBtn || !downBtn) {
        return;
    }

    const min = Number(stepper.dataset.min);
    const max = Number(stepper.dataset.max);
    const step = Number(stepper.dataset.step);

    const clamp = (value) => Math.max(min, Math.min(max, value));

    const pulseValue = () => {

        valueEl.classList.remove("daily-limit-value--pulse");
        void valueEl.offsetWidth;
        valueEl.classList.add("daily-limit-value--pulse");
    };

    const sync = (nextValue, animate = false) => {
        const value = clamp(nextValue);

        input.value = String(value);
        valueEl.textContent = `€ ${value.toFixed(0)}`;
        upBtn.disabled = value >= max;
        downBtn.disabled = value <= min;

        if (animate) {
            pulseValue();
        }
    };

    upBtn.addEventListener("click", () => {
        sync(Number(input.value) + step, true);
    });

    downBtn.addEventListener("click", () => {
        sync(Number(input.value) - step, true);
    });

    valueEl.addEventListener("animationend", (event) => {

        if (event.animationName === "daily-limit-value-pulse") {
            valueEl.classList.remove("daily-limit-value--pulse");
        }
    });

    sync(Number(input.value));
});
