
const searchService = new SearchService();
function SearchService() {

    this.initSearch = (grid, currentConfig) => {
        grid.config.store.subscribe((config) => {
            if (config !== undefined && config.search !== undefined && config.search.keyword !== null
                && config.search.keyword !== undefined && config.search.keyword !== '') {
                console.log("set " + config.search.keyword);
                localStorage.setItem(this.getGridSearchKey(), config.search.keyword);
                currentConfig.search.keyword = config.search.keyword;
            } else {
                localStorage.removeItem(this.getGridSearchKey());
                currentConfig.search.keyword = '';
            }
        });
    }

    this.getGridSearchKey = () => {
        const incidentsGridKey = window.location.pathname;
        return incidentsGridKey + '-incident-search'
    }

    this.getSavedSearch = () => {
        return localStorage.getItem(this.getGridSearchKey());
    }

}
