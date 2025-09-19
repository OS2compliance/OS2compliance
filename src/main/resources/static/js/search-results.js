let filterService;
let paginationService;

function FilterService() {
    this.init = function() {
        const sectionFilterButtons = document.querySelectorAll('.section-filter-btn');
        sectionFilterButtons.forEach(button => {
            button.addEventListener('click', function() {
                const sectionKey = this.dataset.section;
                filterService.filterSections(sectionKey, this);
            });
        });
    }

    /**
     * Filter sections based on selected section key
     * @param {string} selectedSection - The section key to show, or 'all' for all sections
     * @param {HTMLElement} clickedButton - The button that was clicked
     */
    this.filterSections = function(selectedSection, clickedButton) {
        const allSections = document.querySelectorAll('.search-section');
        const allFilterButtons = document.querySelectorAll('.section-filter-btn');
        const visibleSectionsCount = document.querySelector('.visible-sections-count');

        // Update button states
        allFilterButtons.forEach(btn => {
            btn.classList.remove('active');
            if (btn === clickedButton) {
                btn.classList.add('btn-outline-primary');
                btn.classList.remove('btn-outline-secondary');
            } else {
                btn.classList.add('btn-outline-secondary');
                btn.classList.remove('btn-outline-primary');
            }
        });

        // Add active class to clicked button
        clickedButton.classList.add('active');

        let visibleCount = 0;

        // Show/hide sections based on selection
        allSections.forEach(section => {
            const sectionKey = section.dataset.sectionKey;

            if (selectedSection === 'all' || sectionKey === selectedSection) {
                section.style.display = 'block';
                visibleCount++;
            } else {
                section.style.display = 'none';
            }
        });

        // Update visible sections counter
        visibleSectionsCount.textContent = visibleCount;

        // Scroll to first visible section if not showing all
        if (selectedSection !== 'all' && visibleCount > 0) {
            const firstVisibleSection = document.querySelector(`.search-section[data-section-key="${selectedSection}"]`);
            if (firstVisibleSection) {
                firstVisibleSection.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                    inline: 'nearest'
                });
            }
        }
    }
}

function PaginationService() {
    this.init = function() {
        const loadMoreButtons = document.querySelectorAll('.load-more-btn');
        loadMoreButtons.forEach(button => {
            button.addEventListener('click', function() {
                const sectionKey = this.dataset.section;
                const currentPage = parseInt(this.dataset.page);
                const nextPage = currentPage + 1;

                paginationService.loadMoreResults(sectionKey, nextPage, this);
            });
        });
    }

    /**
     * Load more results for a specific section
     * @param {string} sectionKey - The section identifier
     * @param {number} page - The page number to load
     * @param {HTMLElement} button - The button that was clicked
     */
    this.loadMoreResults = function(sectionKey, page, button) {
        const sectionCard = button.closest('[data-section-key]');
        const resultsList = sectionCard.querySelector('.search-results-list');
        const loadingIndicator = sectionCard.querySelector('.loading-indicator');
        const sectionCount = sectionCard.querySelector('.section-count');
        const remainingCount = sectionCard.querySelector('.remaining-count');

        // Show loading state
        button.style.display = 'none';
        loadingIndicator.classList.remove('d-none');

        // Prepare request data
        const params = new URLSearchParams({
            q: searchQuery,
            section: sectionKey,
            page: page
        });

        // Make API request
        fetch(`/rest/search/more?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': token
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // Hide loading indicator
            loadingIndicator.classList.add('d-none');

            // Add new results to the list using template
            data.results.content.forEach(result => {
                const resultElement = paginationService.createResultElementFromTemplate(result, data.displayName, sectionKey);
                resultsList.appendChild(resultElement);
            });

            // Update counters
            const currentShown = resultsList.children.length;
            const totalResults = data.results.totalElements;
            const remaining = totalResults - currentShown;

            sectionCount.textContent = `${currentShown} af ${totalResults}`;
            remainingCount.textContent = remaining;

            // Show/hide load more button
            if (remaining > 0) {
                button.dataset.page = page;
                button.style.display = 'inline-block';
            } else {
                // Hide the entire footer if no more results
                const cardFooter = button.closest('.card-footer');
                if (cardFooter) {
                    cardFooter.style.display = 'none';
                }
            }
        })
        .catch(error => {
            console.error('Error loading more results:', error);

            // Hide loading indicator and show button again
            loadingIndicator.classList.add('d-none');
            button.style.display = 'inline-block';

            // Show error message to user
            toastService.error('Der opstod en fejl ved indlæsning af flere resultater. Prøv igen senere.');
        });
    }

    /**
     * Create result element from HTML template
     * @param {Object} result - The search result data
     * @param {string} displayName - The display name for the section
     * @param {string} sectionKey - The section key for URL mapping
     * @returns {HTMLElement} The created result element
     */
    this.createResultElementFromTemplate = function(result, displayName, sectionKey) {
        const template = document.getElementById('search-result-template');
        const clone = template.content.cloneNode(true);

        // Set the link href based on section type
        const link = clone.querySelector('.result-link');
        link.href = paginationService.getUrlForSection(sectionKey, result.id);

        // Fill in the data
        clone.querySelector('.result-name').innerHTML = result.name;
        clone.querySelector('.result-field-name').textContent = result.searchResultFieldName;
        clone.querySelector('.result-field-content').innerHTML = result.highlightedContent;
        clone.querySelector('.result-display-name').textContent = displayName;

        return clone;
    }

    /**
     * Get URL for a specific section and ID
     * @param {string} sectionKey - The section identifier
     * @param {number} id - The entity ID
     * @returns {string} The URL path
     */
    this.getUrlForSection = function(sectionKey, id) {
        const urlMap = {
            'SUPPLIER': `/suppliers/${id}`,
            'TASK': `/tasks/${id}`,
            'DOCUMENT': `/documents/${id}`,
            'REGISTER': `/registers/${id}`,
            'ASSET': `/assets/${id}`,
            'DBSASSET': `/dbs/assets/${id}`,
            'THREAT_ASSESSMENT': `/risks/${id}`,
            'STANDARD_SECTION': `/standards/supporting/${id}`,
            'INCIDENT': `/incidents/logs/${id}`,
            'DPIA': `/dpia/${id}`
        };

        return urlMap[sectionKey] || '#';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    filterService = new FilterService();
    paginationService = new PaginationService();
    filterService.init();
    paginationService.init();
});