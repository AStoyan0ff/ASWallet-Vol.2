(function () {

    const list = document.querySelector(".risk-reviews-list");
    const items = document.querySelectorAll(".risk-reviews-list-item");
    const panes = document.querySelectorAll(".risk-reviews-detail-pane");

    if (!list || items.length === 0 || panes.length === 0) {
        return;
    }

    function selectReview(reviewId) {

        items.forEach(function (item) {
            const isSelected = item.dataset.reviewId === reviewId;
            item.classList.toggle("is-selected", isSelected);
            item.setAttribute("aria-selected", isSelected ? "true" : "false");
        });

        panes.forEach(function (pane) {
            const isActive = pane.dataset.reviewId === reviewId;
            pane.classList.toggle("is-active", isActive);
            pane.setAttribute("aria-hidden", isActive ? "false" : "true");
        });
    }

    items.forEach(function (item) {

        item.addEventListener("click", function () {
            selectReview(item.dataset.reviewId);
        });

        item.addEventListener("keydown", function (event) {

            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                selectReview(item.dataset.reviewId);
            }
        });
    });

    const initial = list.querySelector(".risk-reviews-list-item.is-selected")
        || items[0];

    if (initial) {
        selectReview(initial.dataset.reviewId);
    }

})();
