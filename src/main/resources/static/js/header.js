document.addEventListener("DOMContentLoaded", function() {
    const burgerBtn = document.getElementById("burger-menu");
    const nav = document.getElementById("mainnav-container");
    let links = document.getElementsByClassName("nav-link");
    const hasSubItems = document.querySelectorAll('.nav-item.has-sub');

    // Create backdrop for overlay mode
    const backdrop = document.createElement('div');
    backdrop.id = "nav-backdrop";
    document.body.appendChild(backdrop);

    function isMobile() {
        return window.innerWidth < 768;
    }

    function isMiniMode() {
        return nav.classList.contains("mini");
    }

    function setupSubMenus() {
        hasSubItems.forEach(function(item) {
            const navLink = item.querySelector('.nav-link');
            const subMenu = item.querySelector('.mininav-content');

            navLink.removeEventListener('click', handleSubMenuClick);

            navLink.addEventListener('click', handleSubMenuClick);

            function handleSubMenuClick(e) {
                if (isMiniMode()) {
                    e.preventDefault();

                    // Close all other sub-menus first
                    hasSubItems.forEach(function(otherItem) {
                        if (otherItem !== item) {
                            otherItem.querySelector('.mininav-content').classList.remove('show');
                        }
                    });

                    subMenu.classList.toggle('show');
                } else {
                    if (subMenu.classList.contains('show')) {
                        subMenu.classList.remove('show');
                    } else {
                        hasSubItems.forEach(function(otherItem) {
                            if (otherItem !== item) {
                                otherItem.querySelector('.mininav-content').classList.remove('show');
                            }
                        });
                        subMenu.classList.add('show');
                    }
                }
            }
        });
    }

    document.addEventListener('click', function(e) {
        const isNavClick = e.target.closest('.mainnav');
        if (!isNavClick) {
            hasSubItems.forEach(function(item) {
                item.querySelector('.mininav-content').classList.remove('show');
            });
        }
    });

    burgerBtn.addEventListener("click", function() {
        if (isMobile()) {
            nav.classList.add("overlay");
            backdrop.classList.add("active");
        } else {
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

            hasSubItems.forEach(function(item) {
                item.querySelector('.mininav-content').classList.remove('show');
            });
        }
    });

    backdrop.addEventListener("click", function() {
        nav.classList.remove("overlay");
        backdrop.classList.remove("active");
    });

    window.addEventListener('load', function () {
        const nav = document.getElementById('mainnav-container');
        if (window.innerWidth < 768) {
            nav.classList.remove('mini', 'full');
        } else {
            nav.classList.add('full');
        }

        setupSubMenus();
    });

    setupSubMenus();
});