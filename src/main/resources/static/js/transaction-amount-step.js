(function (global) {
    global.initTransactionAmountStep = function (amountInput) {

        if (!amountInput) {
            return;
        }

        const STEP = 5;
        const MIN = 0.01;

        const getMax = () => {
            const parsed = parseFloat(amountInput.max);
            return Number.isFinite(parsed) && parsed > 0 ? parsed : 999999.99;
        };

        const parseAmount = (value) => {
            const parsed = parseFloat(String(value).replace(",", "."));
            return Number.isFinite(parsed) ? parsed : null;
        };

        const formatAmount = (value) => (Math.round(value * 100) / 100).toFixed(2);

        const setAmount = (value) => {
            if (value === null || value < MIN) {
                amountInput.value = "";
                return;
            }

            amountInput.value = formatAmount(Math.min(value, getMax()));
        };

        const stepAmount = (direction) => {
            const current = parseAmount(amountInput.value);
            const max = getMax();

            if (direction > 0) {
                if (current === null) {
                    setAmount(Math.min(STEP, max));
                    return;
                }

                setAmount(current + STEP);
                return;
            }

            if (current === null) {
                return;
            }

            const next = current - STEP;

            if (next < MIN) {
                amountInput.value = "";
                return;
            }

            setAmount(next);
        };

        const isSpinnerClick = (event) => {
            if (event.button !== 0) {
                return null;
            }

            const rect = amountInput.getBoundingClientRect();
            const spinnerWidth = 28;

            if (event.clientX < rect.right - spinnerWidth) {
                return null;
            }

            return event.clientY < rect.top + rect.height / 2 ? 1 : -1;
        };

        amountInput.addEventListener("mousedown", (event) => {
            const direction = isSpinnerClick(event);

            if (direction === null) {
                return;
            }

            event.preventDefault();
            stepAmount(direction);
        });

        amountInput.addEventListener("keydown", (event) => {
            if (event.key === "ArrowUp") {
                event.preventDefault();
                stepAmount(1);
            }

            if (event.key === "ArrowDown") {
                event.preventDefault();
                stepAmount(-1);
            }
        });

        amountInput.addEventListener("wheel", (event) => {
            if (document.activeElement !== amountInput) {
                return;
            }

            event.preventDefault();
            stepAmount(event.deltaY < 0 ? 1 : -1);
        }, { passive: false });
    };

})(window);
