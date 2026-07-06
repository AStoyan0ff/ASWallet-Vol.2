document.addEventListener("DOMContentLoaded", () => {

    const card = document.querySelector(".wallet-bank-card");

    if (!card) {
        return;
    }

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    card.classList.add("wallet-bank-card--animate");
    requestAnimationFrame(() => {
        card.classList.add("is-entered");
    });

    if (reducedMotion) {
        return;
    }

    const shine = card.querySelector(".wallet-bank-card__shine");

    const layers =
        [
            {element: card.querySelector(".wallet-bank-card__flow"), depth: 10},
            {element: card.querySelector(".wallet-bank-card__glow"), depth: 16},
            {element: card.querySelector(".wallet-bank-card__orb--one"), depth: 22},
            {element: card.querySelector(".wallet-bank-card__orb--two"), depth: -14}
        ].filter((layer) => layer.element);

    card.classList.add("wallet-bank-card--interactive");

    let targetX = 0;
    let targetY = 0;
    let currentX = 0;
    let currentY = 0;

    const updatePointer = (clientX, clientY) => {

        const rect = card.getBoundingClientRect();
        const x = (clientX - rect.left) / rect.width - 0.5;
        const y = (clientY - rect.top) / rect.height - 0.5;

        targetX = Math.max(-0.5, Math.min(0.5, x));
        targetY = Math.max(-0.5, Math.min(0.5, y));

        if (shine) {
            shine.style.left = `${(targetX + 0.5) * 100}%`;
            shine.style.top = `${(targetY + 0.5) * 100}%`;
        }

        card.classList.add("is-shine-active");
    };

    const resetPointer = () => {

        targetX = 0;
        targetY = 0;
        card.classList.remove("is-shine-active");
    };

    card.addEventListener("pointermove", (event) => {
        updatePointer(event.clientX, event.clientY);
    });

    card.addEventListener("pointerleave", resetPointer);

    const animate = () => {

        currentX += (targetX - currentX) * 0.1;
        currentY += (targetY - currentY) * 0.1;

        layers.forEach(({element, depth}) => {

            const translateX = currentX * depth;
            const translateY = currentY * (depth * 0.72);
            element.style.transform = `translate3d(${translateX}px, ${translateY}px, 0)`;
        });

        requestAnimationFrame(animate);
    };

    requestAnimationFrame(animate);
});
