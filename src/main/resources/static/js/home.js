document.addEventListener("DOMContentLoaded", () => {

    const nav = document.querySelector(".home-nav");
    const revealItems = document.querySelectorAll(".reveal-item");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const isAuthPage = document.body.classList.contains("body-auth");
    const isMaterializePage = isAuthPage
        || document.querySelector(".wallet-panel--materialize") !== null
        || document.querySelector(".del-card--materialize") !== null
        || document.querySelector(".pwd-card--materialize") !== null
        || document.querySelector(".bank-card-panel--materialize") !== null
        || document.querySelector(".home-center-panel--materialize") !== null;

    initAuthMaterialize(isMaterializePage, reducedMotion);
    initHomeBankCards(reducedMotion);

    if (!revealItems.length) {
        return;
    }

    revealItems.forEach((item) => {
        const innerItems = item.querySelectorAll(".reveal-child");

        innerItems.forEach((inner, index) => {
            inner.style.setProperty("--reveal-delay", `${Math.min(index * 70, 420)}ms`);
        });
    });

    if (nav) {

        const updateNavState = () => {
            nav.classList.toggle("is-scrolled", window.scrollY > 14);
        };

        updateNavState();
        window.addEventListener("scroll", updateNavState, {passive: true});
    }

    if (reducedMotion || !("IntersectionObserver" in window)) {

        revealItems.forEach((item) => item.classList.add("is-visible"));
        return;
    }

    if (isMaterializePage) {
        revealItems.forEach((item) => item.classList.add("is-visible"));
        return;
    }

    const observer = new IntersectionObserver(
        (entries, obs) => {
            entries.forEach((entry) => {

                if (!entry.isIntersecting) {
                    return;
                }

                entry.target.classList.add("is-visible");
                obs.unobserve(entry.target);
            });
        },
        {
            threshold: 0.15,
            rootMargin: "0px 0px -40px 0px"
        }
    );

    revealItems.forEach((item, index) => {

        item.style.transitionDelay = `${Math.min(index * 90, 240)}ms`;
        observer.observe(item);
    });
});

function initAuthMaterialize(isAuthPage, reducedMotion) {

    if (!isAuthPage) {
        return;
    }

    const staggerItems = document.querySelectorAll(".auth-stagger");

    staggerItems.forEach((item, index) => {

        const delay = reducedMotion ? 0 : 220 + index * 75;
        item.style.setProperty("--auth-stagger-delay", `${delay}ms`);
    });
}

function initHomeBankCards(reducedMotion) {

    const cards = document.querySelectorAll(".body-home .home-bank-card");

    if (!cards.length || reducedMotion) {
        return;
    }

    cards.forEach((card) => {
        const shine = card.querySelector(".home-bank-card__shine");

        if (!shine) {
            return;
        }

        card.addEventListener("pointermove", (event) => {

            const rect = card.getBoundingClientRect();
            const x = ((event.clientX - rect.left) / rect.width) * 100;
            const y = ((event.clientY - rect.top) / rect.height) * 100;

            shine.style.left = `${x}%`;
            shine.style.top = `${y}%`;
            card.classList.add("is-shine-active");
        });

        card.addEventListener("pointerleave", () => {
            card.classList.remove("is-shine-active");
        });
    });
}
