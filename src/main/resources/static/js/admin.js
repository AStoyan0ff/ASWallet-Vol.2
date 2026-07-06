(function () {

    const manageBtn = document.getElementById("adminManageTool");
    const removeBtn = document.getElementById("adminRemoveTool");
    const removeForm = document.getElementById("adminRemoveForm");
    const hint = document.getElementById("adminUserActionHint");

    if (!manageBtn || !removeBtn) {
        return;
    }

    let selectedCard = null;
    let canManage = false;
    let canRemove = false;

    function setHint(message) {

        if (hint) {
            hint.textContent = message;
        }
    }

    function updateTools() {
        const hasSelection = selectedCard !== null;

        manageBtn.classList.toggle("is-disabled", !hasSelection || !canManage);
        manageBtn.setAttribute("aria-disabled", (!hasSelection || !canManage).toString());

        removeBtn.classList.toggle("is-disabled", !hasSelection || !canRemove);
        removeBtn.setAttribute("aria-disabled", (!hasSelection || !canRemove).toString());

        if (!hasSelection) {
            setHint("Select a user below, then use Manage or Remove.");
            return;
        }

        const username = selectedCard.dataset.username || "user";

        if (!canManage) {
            setHint(username + " is protected and cannot be managed.");
            return;
        }

        if (canRemove) {
            setHint("Selected: " + username + ". Manage or remove this account.");

        } else {
            setHint("Selected: " + username + ". You can manage this account.");
        }
    }

    function selectCard(card) {

        if (selectedCard) {
            selectedCard.classList.remove("admin-user-card--selected");
        }

        selectedCard = card;
        canManage = card.dataset.manageable === "true";
        canRemove = card.dataset.removable === "true";

        card.classList.add("admin-user-card--selected");
        updateTools();
    }

    document.querySelectorAll(".admin-user-card[data-user-id]").forEach(function (card) {

        card.addEventListener("click", function () {
            selectCard(card);
        });

        card.addEventListener("keydown", function (event) {

            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                selectCard(card);
            }
        });
    });

    manageBtn.addEventListener("click", function () {

        if (manageBtn.classList.contains("is-disabled") || !selectedCard || !canManage) {
            return;
        }

        const manageUrl = selectedCard.dataset.manageUrl;

        if (manageUrl) {
            window.location.href = manageUrl;
        }
    });

    removeBtn.addEventListener("click", function () {

        if (removeBtn.classList.contains("is-disabled") || !selectedCard || !canRemove || !removeForm) {
            return;
        }

        const username = selectedCard.dataset.username || "this user";
        const deleteUrl = selectedCard.dataset.deleteUrl;

        if (!deleteUrl) {
            return;
        }

        if (!window.confirm("Are you sure you want to remove " + username + "?")) {
            return;
        }

        removeForm.action = deleteUrl;
        removeForm.submit();
    });

    updateTools();

})();
