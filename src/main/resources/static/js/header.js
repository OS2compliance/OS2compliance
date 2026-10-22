// runs inside #root before nifty.min.js initialises the menu, so it starts in the saved mode
(function() {
    const MENU_STATE_KEY = 'menuState';
    const root = document.getElementById('root');

    if (root && localStorage.getItem(MENU_STATE_KEY) === 'mini') {
        root.classList.replace('mn--max', 'mn--min');
    }

    document.addEventListener("DOMContentLoaded", function() {
        const mainnav = document.getElementById('mainnav-container');
        if (!root || !mainnav) {
            return;
        }

        mainnav.addEventListener('change.nf.sidebar', function() {
            localStorage.setItem(MENU_STATE_KEY, root.classList.contains('mn--min') ? 'mini' : 'full');
        });
    });
})();

document.addEventListener("DOMContentLoaded", function() {

    // version info tooltip
    document.querySelectorAll('.app-version[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));

    // search
    const searchForm = document.querySelector('.searchbox');
    const searchInput = document.getElementById('header-search-input');
    const searchButton = document.querySelector('.searchbox__btn');

    // handle enter
    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            e.preventDefault(); // Forhindrer standard form submit
            performSearch();
        });
    }

    // handle click on search button
    if (searchButton) {
        searchButton.addEventListener('click', function(e) {
            e.preventDefault();
            performSearch();
        });
    }

    // perform search
    function performSearch() {
        const searchTerm = searchInput.value.trim();

        if (searchTerm) {
            window.location.href = `/search-results?q=${encodeURIComponent(searchTerm)}`;
        }
    }
});
