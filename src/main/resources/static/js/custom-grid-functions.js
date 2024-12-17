
/**
 * Enables custom sort, search and pagination for an existing grid.
 * Warning: Overwrites some GridJS configs for the grid, in order to enable the custom features.
 */
class CustomGridFunctions {
    dataUrl
    searchStrings = {}
    grid
    sortDirection
    sortColumn
    page

    /**
     * Enabled custom sort, search and pagination for an existing GridJS object
     * @param {Grid} grid GridJS grid element
     * @param {string} dataUrl Server endpoint handling data request
     * @param {ColumnFieldConfig[]} columnFieldConfig Array of configuration object, each representing a columns search field
     */
    constructor (grid, dataUrl, columnFieldConfig = []){
        this.searchUrl = dataUrl
        this.grid = grid

        //Constructs an object, holding each input elements identifier and current value
        for (const config of columnFieldConfig) {
            updateSearchStrings(
                config.columnName,
                config.valueGetter
            )
        }

        //Update vital config of grid to enable custom search, sort and pagination
        this.grid.updateConfig({
            search: false,
            sort:true,
            pagination: {
                enabled: true,
                limit: 10,
            },
            server: {
                url: dataUrl,
                then: data => data.content,
                total: data => data.total
              }
        })

        // Listen for pagination and sorting changes
        grid.on('pageChange', (page) => update(page, this.sortColumn, this.sortDirection));
        grid.on('sort', (column, direction) => update(this.page, column, direction));
    }

    /**
     * Generates a seearch string based on searched values for columns
     */
    getSearchString() {
        const params =  URLSearchParams()
        params.append("page", this.page)
        params.append("page", this.sortColumn)
        params.append("page", this.sortDirection)
        params.append("page", this.grid.config.pagination.limit)
        for (const [key, value] of this.searchStrings) {
            params.append(key, value())
        }

        return params.toString();
    }

    updateColumnValue(column, value) {
        this.searchStrings[column] = value;
    }

    /**
     * Fetches data from the url, with the current search parameters. Then updates the given grid and forces a re-render
     */
    async update(page, sortColumn, sortDirection) {
        if(page != this.page) {
            this.page = page
        }
        if(sortColumn != this.sortColumn) {
            this.sortColumn = sortColumn
        }
        if(sortDirection != this.sortDirection) {
            this.sortDirection = sortDirection
        }

        const response = await this.#fetchData()
        this.#updateGrid(response)
    }

    /**Fetches sorted, filtered and paginated data from server */
    async #fetchData(){
        const jsonResponse = await fetch(`${this.dataUrl}?${this.getSearchString()}`)
        if (!jsonResponse.ok) {
            throw new Error (`Could not fetch data from ${this.dataUrl}?${getSearchString()}`)
        }

        return await jsonResponse.json()
    }

    /**Rerenders the grid with the given data */
    #updateGrid(data) {
        // Update the GridJS instance with the new data
        grid.updateConfig({
            data: data.content,
            pagination: {
              ...grid.config.pagination,
              total: data.total // Total number of items from the server for pagination
            }
          }).forceRender(); // Force render to update the grid
    }

}

/**
 * Data class used for initialization of CustomGridFunctions
 */
class ColumnFieldConfig {
    /**
     * @param {string} columnName
     * @param {function} valueGetter side-effect free function that retrieves the value of the search field for the column.
     */
    constructor (columnName, valueGetter) {
        this.columnName = columnName,
        this.valueGetter = valueGetter
    }
}
