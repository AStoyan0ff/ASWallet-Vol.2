document.addEventListener("DOMContentLoaded", () => {

    const amountInput = document.getElementById("amount");
    const cvcInput = document.getElementById("cardCvc");
    const form = cvcInput?.closest("form") || amountInput?.closest("form");

    const formatCvc = (value) => value.replace(/\D/g, "").slice(0, 3);

    initTransactionAmountStep(amountInput);

    if (cvcInput) {

        if (cvcInput.value) {
            cvcInput.value = formatCvc(cvcInput.value);
        }

        cvcInput.addEventListener("input", () => {
            cvcInput.value = formatCvc(cvcInput.value);
        });
    }

    if (!form) {
        return;
    }

    form.addEventListener("submit", () => {

        if (cvcInput) {
            cvcInput.value = formatCvc(cvcInput.value);
        }
    });
});
