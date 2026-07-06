document.addEventListener("DOMContentLoaded", () => {

    document.querySelectorAll(".password-field-wrap").forEach((wrap) => {

        const input = wrap.querySelector("input");
        const toggle = wrap.querySelector(".password-visibility-toggle");

        if (!input || !toggle) {
            return;
        }

        toggle.addEventListener("click", () => {

            const show = input.type === "password";

            input.type = show
                ? "text"
                : "password";

            toggle.classList.toggle("is-visible", show);
            toggle.setAttribute("aria-pressed", String(show));
            toggle.setAttribute("aria-label", show
                ? "Hide password"
                : "Show password");
        });
    });
});
