document.addEventListener("DOMContentLoaded", () => {

    const cvcInput = document.getElementById("cardCvc");
    const form = cvcInput?.closest("form");

    if (!form) {
        return;
    }

    const formatCvc = (value) => value.replace(/\D/g, "").slice(0, 3);

    if (cvcInput) {

        if (cvcInput.value) {
            cvcInput.value = formatCvc(cvcInput.value);
        }

        cvcInput.addEventListener("input", () => {
            cvcInput.value = formatCvc(cvcInput.value);
        });
    }

    form.addEventListener("submit", () => {

        if (cvcInput) {
            cvcInput.value = formatCvc(cvcInput.value);
        }
    });
});
