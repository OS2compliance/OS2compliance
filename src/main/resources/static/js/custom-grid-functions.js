
/**
 * Enables custom sort, search and pagination for an existing grid.
 * Warning: Overwrites some GridJS configs for the grid, in order to enable the custom features.
 */
class CustomGridFunctions {
    dataUrl
    grid
    state = {
        sortDirection: 'ASC',
        sortColumn: '',
        page: 0,
        limit: 50,
        searchValues: {}
    }

    /**
     * Enabled custom sort, search and pagination for an existing GridJS object
     * @param {Grid} grid GridJS grid element
     * @param {string} dataUrl Server endpoint handling data request
     */
    constructor(grid, dataUrl) {
        this.dataUrl = dataUrl
        this.grid = grid
        this.state.page = 0
        this.state.limit = 50

        this.loadState()

        //Update vital config of grid to enable custom search, sort and pagination
        const gridConfig = this.grid.updateConfig({
            search: false,
            sort: true,
            server: {
                ...this.grid.config.server,
                url: `${this.dataUrl}?${this.getParamString()}`,
            },
            pagination: {
                enabled: true,
                limit: this.grid.config.pagination && this.grid.config.pagination.limit ? this.grid.config.pagination.limit : this.state.limit,
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

        this.addSearchFields()

        this.grid.on('ready', () => {
            this.loadState()
            this.initializeInputFields()
        })

        gridConfig.forceRender()
    }

    /**
     * Generates a parameters for url request, based on current state
     */
    getParamString() {
        const params = new URLSearchParams()
        if (this.state.page) {
            params.append("page", this.state.page)
        }
        if (this.state.sortColumn) {
            params.append("order", this.state.sortColumn)
        }
        if (this.state.sortDirection) {
            params.append("dir", this.state.sortDirection)
        }
        if (this.state.limit) {
            params.append("limit", this.state.limit)
        }
        for (const [key, value] of Object.entries(this.state.searchValues)) {
            const calculatedValue = value
            if (calculatedValue) {
                params.append(key, calculatedValue)
            }
        }

        return params.toString();
    }

    /**
     * updates state on pagination change
     * @param {URL} prev
     * @param {number} page
     * @param {number} limit
     * @returns
     */
    updatePagination(prev, page, limit) {
        this.state.page = page
        this.limit = limit
        return this.getParamString()
    }

    /**
     * Updates state on sorting change
     * @param {URL} prev
     * @param {object} columns
     * @returns
     */
    updateSorting(prev, columns) {
        if (!columns.length) return this.getParamString();

        const col = columns[0];

        const dir = col.direction === 1 ? 'asc' : 'desc';
        let colName = this.grid.config.columns[col.index].searchable.searchKey;
        this.state.sortColumn = colName
        this.state.sortDirection = dir

        return this.getParamString()
    }

    /**
     * Convenience method for updating column search state
     * @param {string} column
     * @param {string} value
     */
    updateColumnValue(column, value) {
        this.state.searchValues[column] = value;
    }


    /**
     * Adds a text input for every  'searchable' column in the GridJS grid, with id based on the column id
     */
    addSearchFields() {
        const updatedConfig = [...this.grid.config.columns]
        for (const column of updatedConfig) {
            if (!column.hidden && column.searchable) {
                let searchFieldHTML = '<div style="display:none;"></div>'

                if (column.searchable.fieldId) {
                    const foundElement = document.getElementById(column.searchable.fieldId)
                    foundElement.classList.add('grid_columnSearchInput')
                    foundElement.setAttribute('data-search-key', column.searchable.searchKey)
                    searchFieldHTML = foundElement.outerHTML
                    foundElement.remove()
                } else {
                    searchFieldHTML = this.generateTextInputFieldHTML(column.searchable.searchKey)
                }

                column.columns = [{
                    name: gridjs.html(searchFieldHTML),
                    formatter: column.formatter,
                    width: column.width,
                    id: 'search_' + column.id
                }]
            } else if (!column.hidden) {
                column.columns = [{
                    name: gridjs.html(searchFieldHTML),
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

    /**
     * Generates a html input field with the given data-search-key attribute
     * @param {string} searchKey name of search parameter for this field
     * @returns Text input field as HTML
     */
    generateTextInputFieldHTML(searchKey) {
        const inputElement = document.createElement('input')
        inputElement.type = 'text'
        inputElement.classList.add('grid_columnSearchInput')
        inputElement.classList.add('form-control')
        inputElement.setAttribute('data-search-key', searchKey)
        return inputElement.outerHTML
    }

    /**
     * Initializes functionality for search fields
     */
    initializeInputFields() {
        const inputFields = document.getElementsByClassName('grid_columnSearchInput')

        for (const input of inputFields) {
            const key = input.getAttribute('data-search-key')
            const inputType = 'text'

            if (input.tagName === 'select') {inputType = 'enum'}
            if (input.tagName === 'input' && input.type ==='date') {inputType = 'date'}

            input.addEventListener('click', (event) => {
                event.stopPropagation();
                event.preventDefault()
            })

            input.addEventListener('change', (event) => {
                event.stopPropagation();
                event.preventDefault()

console.log(input.value)

                this.updateColumnValue(key,input.value ? input.value : null)

                this.saveState(key, input.value)
                this.onSearch()
            })

            input.addEventListener('keydown', (event) => {
                event.stopPropagation();
            })

            const savedValue = this.state.searchValues[key]
            if (savedValue) {
                input.value = savedValue
            }
        }
    }

    /**
     * Updates the grid based on values of all search fields
     */
    onSearch() {
        this.loadState()

        this.grid.updateConfig({
            server: {
                ...this.grid.config.server,
                url: `${this.dataUrl}?${this.getParamString()}`,
            }
        }).forceRender()
    }

    /**
     * Saves current state to local storage
     */
    saveState() {
        localStorage.setItem(`${this.dataUrl}_search`, JSON.stringify(this.state))
    }

    /**
     * Loads current state from local storage
     */
    loadState() {
        const retrievedState = JSON.parse(localStorage.getItem(`${this.dataUrl}_search`))
        if (retrievedState) {
            this.state = retrievedState
        }
    }
}

class SearchableColumn {
    constructor(searchKey, fieldId = null) {
        this.searchKey = searchKey
        this.fieldId = fieldId
    }
}
