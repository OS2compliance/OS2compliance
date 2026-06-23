import CustomMultiSelect from "./CustomSelector.js";
/**
 * Enables custom sort, search and pagination for an existing grid.
 * Warning: Overwrites some GridJS configs for the grid, in order to enable the custom features.
 */
export default class CustomGridFunctions {
    dataUrl
    grid
    gridId
    state = {
        sortDirection: 'ASC',
        sortColumn: '',
        page: 0,
        limit: 50,
        searchValues: {}
    }
    #INPUTCLASSNAME
    selectorInstances = {}
    _selectGeneration = 0

    /**
     * Enabled custom sort, search and pagination for an existing GridJS object
     * @param {Grid} grid GridJS grid element
     * @param {string} dataUrl Server endpoint handling data request
     * @param gridId
     * @param initialSortConfig config object for the initial sorting of columns
     */
    constructor(
        grid,
        dataUrl,
        gridId,
        initialSortConfig = {
            sortDirection: 'ASC',
            sortColumn: '',
        }) {
        this.dataUrl = dataUrl
        this.grid = grid
        this.state.page = 0
        this.state.limit = 50
        this.gridId = gridId
        this.#INPUTCLASSNAME = `${this.gridId}_grid_columnSearchInput`
        this.state.sortDirection = initialSortConfig.sortDirection || 'ASC'
        this.state.sortColumn = initialSortConfig.sortColumn || ''

        this.loadState()

        const originalThenFunction = this.grid.config.server.then
        //Update vital config of grid to enable custom search, sort and pagination
        const gridConfig = this.grid.updateConfig({
            search: false,
            server: {
                ...this.grid.config.server,
                url: `${this.dataUrl}?${this.getParamString()}`,
                then: (data) => {
                    this.initializeInputFields()
                    this.initializeCustomSelects()
                    return originalThenFunction(data)
                }
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
            },
            language: {
            'noRecordsFound': "Ingen data fundet",
                'pagination': {
                'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'of': 'af',
                    'to': 'til'
            },
        }
        })

        this.addSearchFields()

        if (this.grid.config.container && this.grid.config.container.childNodes.length > 0) {
            gridConfig.forceRender()
        }
    }

    /**
     * Initialize or reinitialize all custom select instances
     */
    initializeCustomSelects() {
        // Destroy all existing instances
        for (let [key, instance] of Object.entries(this.selectorInstances)) {
            instance.destroy();
        }
        this.selectorInstances = {}

        // Small delay to ensure DOM is ready
        const generation = ++this._selectGeneration;
        setTimeout(() => {
            // Avoid creating duplicates when the user clicks fx "next page" before the timeout runs
            if (generation !== this._selectGeneration) {
                return;
            }
            const placeholders = document.querySelectorAll(`#${this.gridId} .custom-select-placeholder`);
            for (let placeholder of placeholders) {
                const originalId = placeholder.dataset.originalId
                const originalSelect = document.getElementById(originalId)

                if (!originalSelect) {
                    console.warn('Original select not found for id', originalId)
                    continue
                }

                const searchKey = this.#getSearchKeyForSelect(originalId)

                // Restore saved selections to original select from state
                if (searchKey && this.state.searchValues[searchKey]) {
                    const savedValue = this.state.searchValues[searchKey]
                    let savedArray = []

                    if (typeof savedValue === 'string') {
                        savedArray = savedValue.split(',').map(v => v.trim()).filter(v => v)
                    } else if (Array.isArray(savedValue)) {
                        savedArray = savedValue
                    }

                    const savedSet = new Set(savedArray)

                    for (let option of Array.from(originalSelect.options)) {
                        option.selected = savedSet.has(option.value)
                    }
                }

                // Create instance - button will be created in placeholder's parent
                const container = placeholder.parentElement
                this.selectorInstances[originalId] = new CustomMultiSelect(container, originalSelect)
            }
        }, 0)
    }

    /**
     * Helper to find the search key associated with a select id
     */
    #getSearchKeyForSelect(selectId) {
        // Look through grid columns to find matching field
        for (const column of this.grid.config.columns) {
            if (column.searchable && column.searchable.fieldId === selectId) {
                return column.searchable.searchKey
            }
        }
        return null
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
            if (Array.isArray(value)) {
                if (value.length > 0) {
                    params.append(key, value.join(','))
                }
            } else if (value) {
                params.append(key, value)
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

        let colName = undefined;
        if (this.grid.config.columns[col.index].searchable) {
            colName = this.grid.config.columns[col.index].searchable.sortKey;
            if (colName === undefined) {
                colName = this.grid.config.columns[col.index].searchable.searchKey;
            }
        }
        this.state.sortColumn = colName
        this.state.sortDirection = dir
        this.saveState();

        return this.getParamString()
    }

    /**
     * Convenience method for updating column search state
     * @param {string} column
     * @param {string} valuef
     */
    updateColumnValue(column, value) {
        if (value === '__EMPTY__') {
            this.state.searchValues[column] = "EMPTY";
        }
        this.state.searchValues[column] = value;
    }


    /**
     * Adds a text input for every  'searchable' column in the GridJS grid, with id based on the column id
     */
    addSearchFields() {
        const updatedConfig = [...this.grid.config.columns]
        for (const column of updatedConfig) {
            let searchFieldHTML = '<div style="display:none;"></div>'

            column.sort = !!column.searchable;

            if (column.searchable && column.searchable.searchKey) {
                if (column.searchable.fieldId) {
                    searchFieldHTML = this.findPredefinedInputFieldHTML(column.searchable.fieldId, column.searchable.searchKey, column.hidden)
                } else {
                    searchFieldHTML = this.generateTextInputFieldHTML(column.searchable.searchKey)
                }

                column.columns = [{
                    name: gridjs.html(searchFieldHTML),
                    formatter: column.formatter,
                    sort: column.sort,
                    width: column.width,
                    hidden: !!column.hidden,
                    id: 'search_' + column.id
                }]
            } else {
                column.columns = [{
                    name: gridjs.html(searchFieldHTML),
                    formatter: column.formatter,
                    sort: column.sort,
                    width: column.width,
                    hidden: !!column.hidden,
                    id: 'search_' + column.id
                }]
            }
            column.onHiddenUpdate = () => {
                for (const subcolumn of column.columns) {
                    subcolumn.hidden = column.hidden
                }
                if (column.hidden && column.searchable !== undefined && column.searchable.searchKey !== undefined) {
                    this.updateColumnValue(column.searchable.searchKey, null)

                    this.saveState(column.searchable.searchKey, null)
                    this.onSearch()
                }
            }
        }
        this.grid.updateConfig({
            columns: updatedConfig
        })
    }

    findPredefinedInputFieldHTML(fieldId, searchKey, isHidden) {
        const foundElement = document.getElementById(fieldId)

        if (!foundElement) {
            return this.generateTextInputFieldHTML(searchKey);
        }

        foundElement.classList.add(this.#INPUTCLASSNAME)
        foundElement.setAttribute('data-search-key', searchKey)

        // Multi-selects become a custom dropdown: hide the original (keep it in
        // the DOM) and emit a placeholder that initializeCustomSelects() turns
        // into a CustomMultiSelect button.
        if (foundElement.hasAttribute('multiple')) {
            foundElement.style.display = 'none'
            return '<div class="custom-select-placeholder" data-original-id="' + fieldId + '"></div>'
        }

        // Single selects keep the original behaviour: inject the element markup
        // into the grid header and let initializeInputFields() wire it up.
        const html = foundElement.outerHTML
        if (isHidden) {
            foundElement.style.display = 'none'
        }
        foundElement.remove()
        return html
    }

    /**
     * Generates a html input field with the given data-search-key attribute
     * @param {string} searchKey name of search parameter for this field
     * @returns Text input field as HTML
     */
    generateTextInputFieldHTML(searchKey) {
        const inputElement = document.createElement('input')
        inputElement.type = 'text'
        inputElement.classList.add(this.#INPUTCLASSNAME)
        inputElement.classList.add('form-control')
        inputElement.setAttribute('data-search-key', searchKey)
        return inputElement.outerHTML
    }

    /**
     * Initializes functionality for search fields
     */
    initializeInputFields() {
        this.loadState()
        const inputFields = document.getElementsByClassName(this.#INPUTCLASSNAME)

        for (const input of inputFields) {
            const key = input.getAttribute('data-search-key')

            let eventTargetElement = input.closest('.gridjs-th-content') || input

            eventTargetElement.addEventListener('click', (event) => {
                event.stopPropagation();

            })

            eventTargetElement.addEventListener('change', (event) => {
                event.stopPropagation();
                this.handleSearchFieldChange(event, key)
            })

            eventTargetElement.addEventListener('keydown', (event) => {
                event.stopPropagation();
            })

            const savedValue = this.state.searchValues[key]
            if (savedValue) {
                input.value = savedValue
            }
        }
    }

    handleSearchFieldChange(event, key) {
        const target = this.#findSearchField(event)
        const value = this.#getSearchFieldValue(target)
        this.updateColumnValue(key, value)

        this.saveState(key, value)
        this.onSearch()
    }

    #getSearchFieldValue(element) {
        if (!element) {
            return null;
        }

        const tagName = element.tagName.toLowerCase();
        if (tagName === 'input') {
            return element.value
        } else if (tagName === 'select') {
            if (element.multiple) {
                return Array.from(element.selectedOptions).map(opt => opt.value);
            } else {
                return element.value;
            }
        }
    }


    #findSearchField(event) {
        const target = event.target
        if (target) {
            let searchField = target.closest(`.${this.#INPUTCLASSNAME}`)
            if (!searchField) {
                const choicesContainer = target.closest('.choices')
                if (choicesContainer) {
                    searchField = choicesContainer.querySelector(`.${this.#INPUTCLASSNAME}`)
                }
            }


            return searchField;
        }
        return null;
    }

    /**
     * Updates the grid based on values of all search fields
     */
    onSearch() {
        // For some reason grid.js reset the column sort after server fetch, or forceRender, so set it again here.
        this.grid.config.columns.forEach(column => {
            column.columns.forEach(subcolumn => {subcolumn.sort = !!column.searchable;})
        })
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

    /**
     * Returns the current filter values (excluding null/empty values)
     * @returns {object} Current search filter values
     */
    getFilters() {
        const filters = {};
        for (const [key, value] of Object.entries(this.state.searchValues)) {
            if (value !== null && value !== undefined && value !== '') {
                filters[key] = value;
            }
        }
        return filters;
    }

    getSortState() {
        // Return current sort state from grid
        return {
            column: this.state.sortColumn || null,
            direction: this.state.sortDirection || 'ASC'
        };
    }
}
