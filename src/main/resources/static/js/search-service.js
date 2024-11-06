
const searchService = new SearchService();
function SearchService() {

    this.initSearch = (grid, currentConfig, gridKeyPrefix = '') => {
        grid.config.store.subscribe((config) => {
            if (config !== undefined && config.search !== undefined && config.search.keyword !== null
                && config.search.keyword !== undefined && config.search.keyword !== '') {
                localStorage.setItem(this.getGridSearchKey(gridKeyPrefix), config.search.keyword);
                currentConfig.search.keyword = config.search.keyword;
            } else {
                localStorage.removeItem(this.getGridSearchKey(gridKeyPrefix));
                currentConfig.search.keyword = '';
            }
        });
    }

    this.getGridSearchKey = (gridKeyPrefix = '') => {
        const incidentsGridKey = window.location.pathname;
        return incidentsGridKey + gridKeyPrefix + '-search'
    }

    this.getSavedSearch = (gridKeyPrefix = '') => {
        return localStorage.getItem(this.getGridSearchKey(gridKeyPrefix));
    }

}
