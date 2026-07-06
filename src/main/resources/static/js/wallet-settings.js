document.addEventListener("DOMContentLoaded", () => {

    const form = document.querySelector(".pref-form");

    if (!form) {
        return;
    }

    const statusEl = document.getElementById("prefSaveStatus");
    const toggles = form.querySelectorAll('.pref-toggle input[type="checkbox"]');
    const settingNames = ["balanceHidden", "emailOnDeposit", "emailOnWithdraw", "emailOnTransfer"];

    let saveTimer = null;
    let saving = false;
    let pendingSave = false;

    const syncToggleVisual = (checkbox) => {
        const label = checkbox.closest(".pref-toggle");

        if (!label) {
            return;
        }

        label.classList.toggle("pref-toggle--on", checkbox.checked);

        const stateEl = label?.closest(".pref-toggle-wrap")?.querySelector(".pref-toggle-state");

        if (stateEl) {

            stateEl.textContent = checkbox.checked ? "ON" : "OFF";
            stateEl.classList.toggle("pref-toggle-state--on", checkbox.checked);
            stateEl.classList.toggle("pref-toggle-state--off", !checkbox.checked);
        }
    };

    const buildSettingsBody = () => {

        const body = new URLSearchParams();
        const csrf = form.querySelector('input[name="_csrf"]');

        if (csrf) {
            body.append(csrf.name, csrf.value);
        }

        settingNames.forEach((name) => {

            const checkbox = form.querySelector(`input[type="checkbox"][name="${name}"]`);
            body.append(name, checkbox && checkbox.checked
                ? "true"
                : "false");
        });

        return body;
    };

    const showStatus = (message, isError) => {

        if (!statusEl) {
            return;
        }

        statusEl.textContent = message;
        statusEl.hidden = false;
        statusEl.classList.toggle("pref-save-status--error", Boolean(isError));
        statusEl.classList.add("pref-save-status--visible");

        window.clearTimeout(showStatus.hideTimer);
        showStatus.hideTimer = window.setTimeout(() => {

            statusEl.classList.remove("pref-save-status--visible");
            statusEl.hidden = true;
        }, 2200);
    };

    const saveSettings = async () => {

        if (saving) {
            pendingSave = true;
            return false;
        }

        saving = true;

        try {
            const response = await fetch(form.action, {

                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-Requested-With": "XMLHttpRequest",
                    "Accept": "application/json"
                },
                body: buildSettingsBody(),
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error("Save failed");
            }

            const data = await response.json();

            if (!data.success) {
                throw new Error("Save failed");
            }

            showStatus("Saved", false);
            return true;

        } catch {
            showStatus("Could not save. Try again.", true);
            return false;

        } finally {
            saving = false;

            if (pendingSave) {
                pendingSave = false;

                saveSettings();
            }
        }
    };

    const queueSave = (rollback) => {
        window.clearTimeout(saveTimer);

        saveTimer = window.setTimeout(async () => {
            const saved = await saveSettings();

            if (!saved && typeof rollback === "function") {
                rollback();
            }
        }, 350);
    };

    toggles.forEach((toggle) => {
        let previousChecked = toggle.checked;

        syncToggleVisual(toggle);

        toggle.addEventListener("pointerdown", () => {
            previousChecked = toggle.checked;
        });

        toggle.addEventListener("change", () => {

            syncToggleVisual(toggle);
            queueSave(() => {

                toggle.checked = previousChecked;
                syncToggleVisual(toggle);
            });
        });
    });
});
