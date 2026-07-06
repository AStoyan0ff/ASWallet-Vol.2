document.addEventListener("DOMContentLoaded", () => {

    const cardInput = document.getElementById("cardNumber");
    const nameInput = document.getElementById("cardholderName");
    const monthInput = document.getElementById("expiryMonth");
    const yearInput = document.getElementById("expiryYear");
    const cvcInput = document.getElementById("cardCvc");
    const form = document.querySelector(".bank-card-form");

    const previewNumber = document.getElementById("cardPreviewNumber");
    const previewHolder = document.getElementById("cardPreviewHolder");
    const previewExpiry = document.getElementById("cardPreviewExpiry");

    if (!form) {
        return;
    }

    const formatCardNumber = (value) => {
        const digits = value.replace(/\D/g, "").slice(0, 16);
        return digits.replace(/(\d{4})(?=\d)/g, "$1 ");
    };

    const formatDigits = (value, maxLength) => value.replace(/\D/g, "").slice(0, maxLength);

    const maskCardNumberPreview = (value) => {
        const digits = value.replace(/\D/g, "").slice(0, 16);

        if (!digits) {
            return "**** **** **** ****";
        }

        const groups = [];

        for (let index = 0; index < 4; index += 1) {
            const chunk = digits.slice(index * 4, index * 4 + 4);

            if (!chunk) {
                groups.push("****");
                continue;
            }

            groups.push(chunk.padEnd(4, "*"));
        }

        return groups.join(" ");
    };

    const updateCardPreview = () => {

        if (previewNumber && cardInput) {
            previewNumber.textContent = maskCardNumberPreview(cardInput.value);
        }

        if (previewHolder && nameInput) {
            const name = nameInput.value.trim();

            previewHolder.textContent = name
                ? name.toUpperCase()
                : "CARDHOLDER NAME";
        }

        if (previewExpiry && monthInput && yearInput) {

            const month = monthInput.value.trim();
            const year = yearInput.value.trim();

            if (month || year) {
                previewExpiry.textContent = `${month || "MM"}/${year || "YY"}`;

            } else {
                previewExpiry.textContent = "MM/YY";
            }
        }
    };

    const clearValidity = (input) => {

        if (input) {
            input.setCustomValidity("");
        }
    };

    if (cardInput) {

        if (cardInput.value) {
            cardInput.value = formatCardNumber(cardInput.value);
        }

        cardInput.addEventListener("input", () => {
            clearValidity(cardInput);

            const cursorFromEnd = cardInput.value.length - cardInput.selectionStart;
            cardInput.value = formatCardNumber(cardInput.value);
            const newPosition = Math.max(cardInput.value.length - cursorFromEnd, 0);
            cardInput.setSelectionRange(newPosition, newPosition);
            updateCardPreview();
        });
    }

    if (nameInput) {

        nameInput.addEventListener("input", () => {
            clearValidity(nameInput);
            updateCardPreview();
        });
    }

    if (monthInput) {

        monthInput.addEventListener("input", () => {
            clearValidity(monthInput);
            monthInput.value = formatDigits(monthInput.value, 2);
            updateCardPreview();
        });
    }

    if (yearInput) {

        yearInput.addEventListener("input", () => {
            clearValidity(yearInput);
            yearInput.value = formatDigits(yearInput.value, 2);
            updateCardPreview();
        });
    }

    if (cvcInput) {

        cvcInput.addEventListener("input", () => {
            clearValidity(cvcInput);
            cvcInput.value = formatDigits(cvcInput.value, 3);
        });
    }

    updateCardPreview();

    form.addEventListener("submit", () => {

        if (cardInput) {
            cardInput.value = cardInput.value.replace(/\D/g, "");
        }

        if (nameInput) {
            nameInput.value = nameInput.value.trim();
        }

        if (cvcInput) {
            cvcInput.value = formatDigits(cvcInput.value, 3);
        }
    });
});
