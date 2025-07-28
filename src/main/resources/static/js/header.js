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
        return window.innerWidth < 993;
    }

    function isMiniMode() {
        return nav.classList.contains("mini");
    }

    // Save the state
    function saveMenuState(state) {
        localStorage.setItem('menuState', state);
    }

    // Get saved menu state from localStorage
    function getSavedMenuState() {
        return localStorage.getItem('menuState') || 'full';
    }

    function applyMenuState(state) {
        if (isMobile()) {
            // On mobile, always remove mini/full classes
            nav.classList.remove('mini', 'full');
            return;
        }

        if (state === 'mini') {
            nav.classList.remove('full');
            nav.classList.add('mini');

            // Apply mini styling to links
            for (let i = 0; i < links.length; i++) {
                links[i].classList.add('active2');
                links[i].classList.remove('inactive');
            }
        } else {
            nav.classList.remove('mini');
            nav.classList.add('full');

            // Apply full styling to links
            for (let i = 0; i < links.length; i++) {
                links[i].classList.add('inactive');
                links[i].classList.remove('active2');
            }
        }

        // Close any open submenus when changing state
        hasSubItems.forEach(function(item) {
            item.querySelector('.mininav-content').classList.remove('show');
        });
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
            const currentState = getSavedMenuState();
            const newState = currentState === 'mini' ? 'full' : 'mini';

            // Save the new state
            saveMenuState(newState);

            // Apply the new state
            applyMenuState(newState);
        }
    });

    backdrop.addEventListener("click", function() {
        nav.classList.remove("overlay");
        backdrop.classList.remove("active");
    });

    function initializeMenuState() {
        if (isMobile()) {
            // On mobile, remove any mini/full classes
            nav.classList.remove('mini', 'full');
        } else {
            // On desktop, apply saved state
            const savedState = getSavedMenuState();
            applyMenuState(savedState);
        }
        setupSubMenus();
    }

    window.addEventListener('load', function () {
        initializeMenuState();
    });

    initializeMenuState();
});