function normalizeIban(iban) {

    return (iban || "").replace(/\s/g, "").toUpperCase();
}

function formatIban(iban) {
    const raw = normalizeIban(iban);

    if (raw.length !== 22) {
        return iban;
    }

    return `${raw.slice(0, 4)} ${raw.slice(4, 8)} ${raw.slice(8, 12)} ${raw.slice(12, 16)} ${raw.slice(16, 20)} ${raw.slice(20, 22)}`;
}

const IBAN_MASKED_DISPLAY = "**** **** **** ****";

function maskIban() {
    return IBAN_MASKED_DISPLAY;
}

document.addEventListener("DOMContentLoaded", () => {
    const ibanBlock = document.querySelector(".wallet-iban");

    if (!ibanBlock) {
        return;
    }

    const valueEl = ibanBlock.querySelector(".wallet-iban-value");
    const toggleBtn = ibanBlock.querySelector(".wallet-iban-toggle");
    const fullIban = ibanBlock.dataset.iban;

    if (!valueEl || !toggleBtn || !fullIban) {
        return;
    }

    const formattedIban = formatIban(fullIban);
    const maskedIban = maskIban();
    let visible = false;

    const setVisible = (nextVisible) => {
        visible = nextVisible;

        valueEl.textContent = visible
            ? formattedIban
            : maskedIban;

        toggleBtn.classList.toggle("is-visible", visible);

        toggleBtn.setAttribute("aria-pressed", visible
            ? "true"
            : "false");

        toggleBtn.setAttribute("aria-label", visible
            ? "Hide IBAN"
            : "Show IBAN");
    };

    setVisible(true);

    toggleBtn.addEventListener("click", () => {
        setVisible(!visible);
    });
});
