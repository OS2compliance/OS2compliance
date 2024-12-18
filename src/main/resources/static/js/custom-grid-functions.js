
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
    limit

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
        this.limit = 50

        console.log(grid)

        //Update vital config of grid to enable custom search, sort and pagination
        const gridConfig = this.grid.updateConfig({
            search: false,
            sort: true,
            pagination: {
                enabled: true,
                limit: this.grid.config.pagination && this.grid.config.pagination.limit ? this.grid.config.pagination.limit : this.limit,
                server: {
                    url: (prev, page, limit) => `${this.dataUrl}?${this.updatePagination(prev, page, limit)}`
                }
            },
            sort: {
                server: {
                    url: (prev, columns) => `${this.dataUrl}?${this.updateSorting(prev, columns)}`
                }
            }
        })

        this.grid.on('ready', () => this.initializeInputFields())

        this.addSearchFields()

        gridConfig.forceRender()
    }

    /**
     * Generates a seearch string based on searched values for columns
     */
    getParamString() {
        const params = new URLSearchParams()
        if (this.page) {
            params.append("page", this.page)
        }
        if (this.sortColumn) {
            params.append("order", this.sortColumn)
        }
        if (this.sortDirection) {
            params.append("dir", this.sortDirection)
        }
        if (this.limit) {
            params.append("limit", this.limit)
        }
        for (const [key, value] of Object.entries(this.searchStrings)) {
            const calculatedValue = value()
            if (calculatedValue) {
                params.append(key, calculatedValue)
            }
        }

        return params.toString();
    }

    updatePagination(prev, page, limit) {
        this.page = page
        this.limit = limit
        return this.getParamString()
    }

    updateSorting(prev, columns) {
        if (!columns.length) return this.getParamString();

        const col = columns[0];

        const dir = col.direction === 1 ? 'asc' : 'desc';
        let colName = this.grid.config.columns[col.index].searchable;
        this.sortColumn = colName
        this.sortDirection = dir

        return this.getParamString()
    }

    updateColumnValue(column, value) {
        this.searchStrings[column] = value;
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

        const gridConfig = this.grid.updateConfig({
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

        for (const input of inputFields) {
            input.addEventListener('click', (event) => {
                event.stopPropagation();
                event.preventDefault()
            })

            input.addEventListener('change', (event) => {
                event.stopPropagation();
                event.preventDefault()
                this.updateColumnValue(input.getAttribute('data-search-key'), () => input.value)
                this.onSearch()
            })

            input.addEventListener('keydown', (event) => {
                event.stopPropagation();
            })
        }
    }

    onSearch(){
        //get values for input fields
        //update the url
        for ( const [key,value] of Object.entries(this.searchStrings)) {
            console.log(key, value())
        }
        this.grid.updateConfig({
            server : {
                 ...this.grid.config.server,
                 url: `${this.dataUrl}?${this.getParamString()}`,
            }
        }).forceRender()
    }

    static isSearchable(columnName) {
        return (cell, row, column) => {
            if (!cell) {
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
