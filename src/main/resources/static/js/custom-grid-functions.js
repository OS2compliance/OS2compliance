
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
    constructor(grid, dataUrl) {
        this.dataUrl = dataUrl
        this.grid = grid
        this.page = 0

        //Update vital config of grid to enable custom search, sort and pagination
        this.grid.updateConfig({
            search: false,
            sort: true,
            pagination: {
                enabled: true,
                limit: 50,
            },
        })

        this.addSearchFields()

        // Listen for pagination and sorting changes
        this.grid.on('pageChange', (page) => this.update(page, this.sortColumn, this.sortDirection));
        this.grid.on('sort', (column, direction) => {
            this.update(this.page, column, direction)
        });

        this.update(this.page, this.column, this.direction)

    }

    /**
     * Generates a seearch string based on searched values for columns
     */
    getSearchString() {
        const params = new URLSearchParams()
        if (this.page) {
            params.append("page", this.page)
        }
        if (this.sortColumn) {
            params.append("sortColumn", this.sortColumn)
        }
        if (this.sortDirection) {
            params.append("sortDirection", this.sortDirection)
        }
        if (this.grid.config.pagination.limit) {
            params.append("limit", this.grid.config.pagination.limit)
        }
        for (const [key, value] of Object.entries(this.searchStrings)) {
            const calculatedValue = value()
            if (calculatedValue) {
                params.append(key, calculatedValue)
            }
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
        if (page != this.page) {
            this.page = page
        }
        if (sortColumn != this.sortColumn) {
            this.sortColumn = sortColumn
        }
        if (sortDirection != this.sortDirection) {
            this.sortDirection = sortDirection
        }

        this.grid.updateConfig({
            server: {
                ...this.grid.config.server,
                url: `${this.dataUrl}?${this.getSearchString()}`,

                //                pagination: {
                //                    enabled: true,
                //                    ...this.grid.config.pagination,
                //                    total: data.total // Total number of items from the server for pagination
                //                }
            },
        }).forceRender();

        setTimeout(() => {
            this.initializeInputFields()
        }, 1000)
    }

    addSearchFields() {



        const updatedConfig = [...this.grid.config.columns]
        for (const column of updatedConfig) {
            if (!column.hidden && column.searchable) {
                column.columns = [
                    {
                        name: gridjs.html(this.generateInputField(column.searchable)),
                        formatter: column.formatter,
                        width: column.width,
                        id: 'search_' + column.id
                    }]
            }
        }
        this.grid.updateConfig({
            columns: updatedConfig
        })
    }

    generateInputField(searchKey) {
        const inputElement = document.createElement('input')
        inputElement.type = 'text'
        inputElement.classList.add('grid_columnSearchInput')
        inputElement.classList.add('form-control')
        inputElement.setAttribute('data-search-key', searchKey)
        return inputElement.outerHTML
    }

    initializeInputFields() {
        const inputFields = document.getElementsByClassName('grid_columnSearchInput')
        console.log('fields', inputFields)
        console.log('fields', inputFields.length)

        for (const input of inputFields) {
            console.log('adding listener')
            input.addEventListener('click', (event) => {
                event.stopPropagation();
                event.preventDefault()
            })

            input.addEventListener('change', () => {
                this.updateColumnValue(input.getAttribute('data-search-key'), () => input.value)
                this.update(this.page, this.sortColumn, this.sortDirection)
            })
        }
    }

    static isSearchable(columnName) {
        return (cell, row, column) => {
            if (!cell) {
                console.log(column)
                return {
                    'data-searchable': `${columnName}`
                }
            }
        }
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
    constructor(columnName, valueGetter) {
        this.columnName = columnName,
            this.valueGetter = valueGetter
    }
}
