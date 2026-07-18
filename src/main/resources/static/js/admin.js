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
            setHint("Select a user, then Manage or Remove.");
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

    function syncActionButtonWidth() {

        const toolBtn = document.querySelector(".admin-tools-grid--left .wallet-quick-action");
        const split = document.querySelector(".admin-split");

        if (!toolBtn || !split) {
            return;
        }

        const width = Math.round(toolBtn.getBoundingClientRect().width);

        if (width > 0) {
            split.style.setProperty("--admin-tool-btn-width", width + "px");
        }
    }

    function updateUserListScroll() {

        const scroll = document.querySelector(".admin-split__pane--right .admin-user-list-scroll");

        if (!scroll) {
            return;
        }

        const list = scroll.querySelector(".admin-user-list");
        const cards = list ? Array.from(list.querySelectorAll(".admin-user-card[data-user-id]")) : [];

        if (cards.length <= 4) {

            scroll.classList.remove("is-scrollable");
            scroll.style.maxHeight = "";

            return;
        }

        const styles = window.getComputedStyle(list);
        const gap = parseFloat(styles.rowGap || styles.gap || "0") || 0;
        let height = 0;

        for (let i = 0; i < 4; i++) {
            height += cards[i].offsetHeight;
        }

        height += gap * 3;

        scroll.style.maxHeight = Math.ceil(height) + "px";
        scroll.classList.add("is-scrollable");
    }

    updateTools();
    updateUserListScroll();
    syncActionButtonWidth();

    window.addEventListener("resize", function () {

        updateUserListScroll();
        syncActionButtonWidth();
    });

})();
