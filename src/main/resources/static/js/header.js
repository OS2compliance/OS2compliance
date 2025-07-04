document.addEventListener("DOMContentLoaded", function() {
    const burgerBtn = document.getElementById("burger-menu");
    const nav = document.getElementById("mainnav-container");
    let links = document.getElementsByClassName("nav-link");

    // Create backdrop for overlay mode
    const backdrop = document.createElement('div');
    backdrop.id = "nav-backdrop";
    document.body.appendChild(backdrop);

    function isMobile() {
        return window.innerWidth < 768;
    }

    burgerBtn.addEventListener("click", function() {
        if (isMobile()) {
            nav.classList.add("overlay");
            backdrop.classList.add("active");
        } else {
            // Desktop toggle full/icon
            if (nav.classList.contains("mini")) {
                nav.classList.remove("mini");
                nav.classList.add("full");

                for (let i = 0; i < links.length; i++) {
                    links[i].classList.add("inactive");
                    links[i].classList.remove("active2");
                }

            } else {
                nav.classList.remove("full");
                nav.classList.add("mini");

                for (let i = 0; i < links.length; i++) {
                    links[i].classList.add("active2");
                    links[i].classList.remove("inactive");
                }
            }
        }
    });

    backdrop.addEventListener("click", function() {
        nav.classList.remove("overlay");
        backdrop.classList.remove("active");
    });

    // Set initial state on load
    window.addEventListener('load', function () {
        const nav = document.getElementById('mainnav-container');
        if (window.innerWidth < 768) {
            nav.classList.remove('mini', 'full');
        } else {
            nav.classList.add('full');
        }
    });
})

