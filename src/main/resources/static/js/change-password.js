document.addEventListener("DOMContentLoaded", () => {

    const oldPasswordInput = document.getElementById("oldPassword");
    const newPasswordInput = document.getElementById("newPassword");
    const strengthLabel = document.getElementById("pwdStrengthLabel");
    const strengthBar = document.getElementById("pwdStrengthBar");
    const requirementsList = document.getElementById("pwdRequirements");
    const samePasswordError = document.getElementById("samePasswordError");
    const form = newPasswordInput?.closest("form");

    if (!newPasswordInput || !strengthLabel || !strengthBar || !requirementsList) {
        return;
    }

    const isDifferentFromCurrent = () => {

        if (!oldPasswordInput) {
            return true;
        }

        const oldValue = oldPasswordInput.value;
        const newValue = newPasswordInput.value;

        return oldValue.length === 0 || newValue.length === 0 || oldValue !== newValue;
    };

    const rules = {

        length: (value) => value.length >= 8,
        upper: (value) => /[A-Z]/.test(value),
        lower: (value) => /[a-z]/.test(value),
        number: (value) => /\d/.test(value),
        special: (value) => /[^A-Za-z0-9]/.test(value),
        different: () => isDifferentFromCurrent()
    };

    const strengthLabels = ["Weak", "Fair", "Good", "Strong", "Strong"];

    const updateSamePasswordError = () => {

        if (!samePasswordError) {
            return;
        }

        const showError = oldPasswordInput
            && oldPasswordInput.value.length > 0
            && newPasswordInput.value.length > 0
            && oldPasswordInput.value === newPasswordInput.value;

        samePasswordError.hidden = !showError;
    };

    const updateStrength = () => {

        const value = newPasswordInput.value;
        const passed = Object.keys(rules).filter((key) => rules[key](value)).length;

        requirementsList.querySelectorAll("li[data-rule]").forEach((item) => {

            const rule = item.getAttribute("data-rule");
            item.classList.toggle("is-met", rules[rule](value));
        });

        strengthBar.querySelectorAll(".pwd-strength-seg").forEach((segment, index) => {
            segment.classList.toggle("is-filled", index < passed);
        });

        strengthLabel.textContent = value.length === 0
            ? "Weak"
            : strengthLabels[Math.max(0, passed - 1)];

        strengthLabel.classList.toggle("is-strong", passed >= 4);
        updateSamePasswordError();
    };

    newPasswordInput.addEventListener("input", updateStrength);

    if (oldPasswordInput) {
        oldPasswordInput.addEventListener("input", updateStrength);
    }

    if (form) {
        form.addEventListener("submit", (event) => {

            if (!isDifferentFromCurrent()) {
                event.preventDefault();
                updateSamePasswordError();
            }
        });
    }

    updateStrength();
});
