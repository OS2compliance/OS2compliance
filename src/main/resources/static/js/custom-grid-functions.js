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

    initializeCustomSelects() {
        const generation = ++this._selectGeneration;
        setTimeout(() => {
            if (generation !== this._selectGeneration) {
                return;
            }
            for (const container of document.querySelectorAll(`#${this.gridId} [data-multiselect-id]`)) {
                const fieldId = container.dataset.multiselectId;
                const searchKey = container.dataset.searchKey;
                const select = document.getElementById(fieldId);
                const button = container.querySelector('button');
                const checkboxes = container.querySelectorAll('input[type="checkbox"]');

                if (!select) {
                    console.warn('Original select not found for id', fieldId);
                    continue;
                }

                // Restore saved selections
                const savedValue = this.state.searchValues[searchKey];
                if (savedValue) {
                    const savedArray = typeof savedValue === 'string'
                        ? savedValue.split(',').map(v => v.trim()).filter(v => v)
                        : (Array.isArray(savedValue) ? savedValue : []);
                    const savedSet = new Set(savedArray);
                    for (const option of select.options) {
                        option.selected = savedSet.has(option.value);
                    }
                    for (const checkbox of checkboxes) {
                        checkbox.checked = savedSet.has(checkbox.value);
                    }
                }

                this.#updateMultiSelectButtonText(button, select);

                const bsDropdown = bootstrap.Dropdown.getOrCreateInstance(button, {
                    autoClose: false,
                    popperConfig: { strategy: 'fixed' }
                });

                let outsideClickHandler = null;

                container.querySelector('.dropdown-menu').addEventListener('click', (e) => {
                    e.stopPropagation();
                });

                button.addEventListener('click', (e) => {
                    e.stopPropagation();
                    bsDropdown.toggle();
                });

                button.addEventListener('show.bs.dropdown', () => {
                    container.querySelector('.dropdown-menu').style.width = `${button.offsetWidth}px`;
                    for (const checkbox of checkboxes) {
                        checkbox.checked = Array.from(select.options).find(o => o.value === checkbox.value)?.selected ?? false;
                    }
                    outsideClickHandler = (e) => {
                        if (!container.contains(e.target)) {
                            bsDropdown.hide();
                        }
                    };
                    document.addEventListener('click', outsideClickHandler, true);
                });

                button.addEventListener('hide.bs.dropdown', () => {
                    document.removeEventListener('click', outsideClickHandler, true);
                    outsideClickHandler = null;
                });

                button.addEventListener('hidden.bs.dropdown', () => {
                    const checkedValues = new Set(Array.from(checkboxes).filter(cb => cb.checked).map(cb => cb.value));
                    let changed = false;
                    for (const option of select.options) {
                        const shouldBeSelected = checkedValues.has(option.value);
                        if (option.selected !== shouldBeSelected) {
                            option.selected = shouldBeSelected;
                            changed = true;
                        }
                    }
                    if (changed) {
                        this.#updateMultiSelectButtonText(button, select);
                        const values = Array.from(checkedValues);
                        this.updateColumnValue(searchKey, values.length > 0 ? values : null);
                        this.saveState();
                        this.onSearch();
                    }
                });
            }
        }, 0);
    }

    #updateMultiSelectButtonText(button, select) {
        const selected = Array.from(select.selectedOptions);
        if (selected.length === 0) {
            button.textContent = 'Intet filter';
        } else if (selected.length === 1) {
            button.textContent = selected[0].text;
        } else {
            button.textContent = `${selected.length} valgt`;
        }
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

        if (foundElement.hasAttribute('multiple')) {
            foundElement.style.display = 'none';
            const dropdown = document.querySelector(`[data-multiselect-for="${fieldId}"]`);
            if (!dropdown) {
                console.warn('No pre-rendered dropdown found for multi-select', fieldId);
                return '<div></div>';
            }
            dropdown.dataset.multiselectId = fieldId;
            dropdown.dataset.searchKey = searchKey;
            delete dropdown.dataset.multiselectFor;
            const html = dropdown.outerHTML;
            dropdown.remove();
            return html;
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
