/**
 * Adds server-side column search, sort, and pagination to an existing GridJS grid.
 * GridJS's built-in search operates client-side and doesn't compose well with server-side
 * pagination, so we override those configs and inject our own filter inputs instead.
 */
class CustomGridFunctions {
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

    /**
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
        this.initialSortConfig = initialSortConfig
        this.state.sortDirection = initialSortConfig.sortDirection || 'ASC'
        this.state.sortColumn = initialSortConfig.sortColumn || ''

        this.loadState()

        // Must run before the initial url is built below, so seeded default values
        // (e.g. from a select's HTML `selected` option) are included in the first request.
        this.addSearchFields()

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

        if (this.grid.config.container && this.grid.config.container.childNodes.length > 0) {
            gridConfig.forceRender()
        }
    }

    /**
     * GridJS destroys and recreates the DOM on every render, so multiselect widgets
     * must be fully re-initialized each time. We use an AbortController to tear down
     * all previous listeners in one call rather than tracking individual references,
     * which would leak across renders.
     */
    initializeCustomSelects() {
        this._selectAbortController?.abort();
        this._selectAbortController = new AbortController();
        const { signal } = this._selectAbortController;

        for (const container of document.querySelectorAll(`#${this.gridId} [data-multiselect-id]`)) {
            this.#initializeCustomSelect(container, signal);
        }
    }

    /**
     * Initializes a multiselect widget.
     *
     * `autoClose: false` is required because Bootstrap would otherwise close the
     * dropdown on any click inside it before we can read the checkbox state.
     * `strategy: 'fixed'` prevents the dropdown from being clipped by the table
     * header's `overflow: hidden`.
     */
    #initializeCustomSelect(container, signal) {
        const fieldId = container.dataset.multiselectId;
        const searchKey = container.dataset.searchKey;
        const select = document.getElementById(fieldId);
        const button = container.querySelector('button');
        const checkboxes = container.querySelectorAll('input[type="checkbox"]');

        if (!select) {
            console.warn('Original select not found for id', fieldId);
            return;
        }

        this.#applySavedMultiSelectValue(select, checkboxes, searchKey);
        this.#updateMultiSelectButtonText(button, select);

        const bsDropdown = bootstrap.Dropdown.getOrCreateInstance(button, {
            autoClose: false,
            popperConfig: { strategy: 'fixed' }
        });

        this.#bindMultiSelectEvents({ container, select, button, checkboxes, searchKey, bsDropdown, signal });
    }

    /**
     * Applies the saved value from `this.state` to the multiselect widget.
     *
     * The grid DOM is recreated on every render, so the filter state cannot be read
     * from the DOM — it must come from `this.state`, which survives across renders.
     */
    #applySavedMultiSelectValue(select, checkboxes, searchKey) {
        const savedValue = this.state.searchValues[searchKey];
        if (!savedValue) {
            return;
        }

        let savedArray = [];
        if (typeof savedValue === 'string') {
            savedArray = savedValue.split(',').map(v => v.trim()).filter(Boolean);
        } else if (Array.isArray(savedValue)) {
            savedArray = savedValue;
        }
        const savedSet = new Set(savedArray);

        for (const option of select.options) {
            option.selected = savedSet.has(option.value);
        }
        for (const checkbox of checkboxes) {
            checkbox.checked = savedSet.has(checkbox.value);
        }
        this.#syncDropdownItemActiveStates(checkboxes);
    }

    /**
     * Binds event handlers to the multiselect widget.
     *
     * Key decisions:
     * - `stopPropagation` on the menu prevents Bootstrap's document-level handler
     *   from treating inner clicks as "outside" clicks and closing the dropdown.
     * - Width is set manually on `show` because `position: fixed` breaks CSS
     *   percentage widths relative to the parent element.
     * - Changes are committed on `hidden` (not per-checkbox) so all selections
     *   result in a single search request instead of one per checkbox.
     */
    #bindMultiSelectEvents({ container, select, button, checkboxes, searchKey, bsDropdown, signal }) {
        container.querySelector('.dropdown-menu').addEventListener('click', (e) => {
            e.stopPropagation();
        }, { signal });

        button.addEventListener('click', (e) => {
            e.stopPropagation();
            bsDropdown.toggle();
        }, { signal });

        button.addEventListener('show.bs.dropdown', () => {
            // position:fixed breaks CSS % width, so match button width manually here
            container.querySelector('.dropdown-menu').style.width = `${button.offsetWidth}px`;
            for (const checkbox of checkboxes) {
                checkbox.checked = Array.from(select.options).find(o => o.value === checkbox.value)?.selected ?? false;
            }
            this.#syncDropdownItemActiveStates(checkboxes);
        }, { signal });

        document.addEventListener('click', (e) => {
            if (!container.contains(e.target)) {
                bsDropdown.hide();
            }
        }, { capture: true, signal });

        button.addEventListener('hidden.bs.dropdown', () => {
            this.#commitMultiSelectChanges(container, select, button, checkboxes, searchKey);
        }, { signal });

        for (const checkbox of checkboxes) {
            checkbox.addEventListener('change', () => {
                checkbox.closest('.dropdown-item')?.classList.toggle('active', checkbox.checked);
            }, { signal });
        }
    }

    /**
     * Applies the selected values from the multiselect widget to the hidden
     * `<select>` and updates the button text.
     *
     * Diffing against the hidden `<select>` before committing avoids triggering a
     * server re-fetch when the user opens and closes the dropdown without changing
     * anything. The container guard handles the edge case where the grid re-renders
     * while the dropdown is open.
     */
    #commitMultiSelectChanges(container, select, button, checkboxes, searchKey) {
        if (!document.contains(container)) {
            return;
        }

        const checkedValues = new Set(Array.from(checkboxes).filter(cb => cb.checked && cb.value !== '').map(cb => cb.value));
        let changed = false;
        for (const option of select.options) {
            const shouldBeSelected = checkedValues.has(option.value);
            if (option.selected !== shouldBeSelected) {
                option.selected = shouldBeSelected;
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        this.#updateMultiSelectButtonText(button, select);
        const values = Array.from(checkedValues);
        this.updateColumnValue(searchKey, values.length > 0 ? values : null);
        this.saveState();
        this.onSearch();
    }

    /**
     * Toggle active/inactive states for dropdown items based on the checked state
     * of their corresponding checkboxes.
     */
    #syncDropdownItemActiveStates(checkboxes) {
        for (const checkbox of checkboxes) {
            checkbox.closest('.dropdown-item')?.classList.toggle('active', checkbox.checked);
        }
    }

    /**
     * Updates the multiselect button text based on the selected options.
     */
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
     * Generates parameters for url request, based on the current state.
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
     * @param {string} value
     */
    updateColumnValue(column, value) {
        // '__EMPTY__' (option-værdien for "Ingen") gemmes uoversat, så gendannelse af gemt søgning
        // rammer selectorens option igen - serveren oversætter til EMPTY (FilterService).
        this.state.searchValues[column] = value;
    }


    /**
     * Adds a text input for every  'searchable' column in the GridJS grid, with id based on the column id
     */
    addSearchFields() {
        const updatedConfig = [...this.grid.config.columns]
        for (const column of updatedConfig) {
            let searchFieldHTML = '<div style="display:none;"></div>'

            // An explicit `sortKey: null` means "filterable but not sortable" — without this the
            // header still renders a sort arrow, and clicking it silently discards the user's
            // current sort while the server falls back to its default order.
            column.sort = !!column.searchable && column.searchable.sortKey !== null;

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

    /**
     * For multiselects: the dropdown wrapper is moved (not cloned) into the grid
     * header as an HTML string, so `initializeCustomSelects` can re-wire it after
     * each render. The original `<select>` stays in the DOM but hidden, acting as
     * the source of truth for selected values.
     * For single selects: the original element is removed to prevent duplicate IDs
     * after GridJS injects the captured outerHTML into the header.
     */
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
            this.#seedDefaultSearchValue(searchKey, Array.from(foundElement.selectedOptions).map(o => o.value))
            dropdown.dataset.multiselectId = fieldId;
            dropdown.dataset.searchKey = searchKey;
            delete dropdown.dataset.multiselectFor;
            const html = dropdown.outerHTML;
            dropdown.remove();
            return html;
        }

        // Single selects keep the original behaviour: inject the element markup
        // into the grid header and let initializeInputFields() wire it up.
        this.#seedDefaultSearchValue(searchKey, foundElement.value)
        const html = foundElement.outerHTML
        if (isHidden) {
            foundElement.style.display = 'none'
        }
        foundElement.remove()
        return html
    }

    /**
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

    /**
     * Single entry point for all search field changes so state update, persistence,
     * and re-fetch always happen together and in the right order.
     */
    handleSearchFieldChange(event, key) {
        const target = this.#findSearchField(event)
        const value = this.#getSearchFieldValue(target)
        this.updateColumnValue(key, value)

        this.saveState(key, value)
        this.onSearch()
    }

    /**
     * Returns an array for multi-selects so `getParamString` can join the values
     * as a single comma-separated parameter rather than repeating the key.
     */
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
        } else {
            return null;
        }
    }


    /**
     * Choices.js wraps the underlying `<select>` in its own container, so
     * `event.target` may be a Choices.js element rather than the input we track.
     * The fallback queries within the Choices wrapper to find the actual field.
     */
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
     * Seeds a search field's state with its default DOM value, but only if no
     * value was already restored from persisted state — a saved filter must
     * take priority over the field's static HTML default.
     */
    #seedDefaultSearchValue(key, value) {
        if (!key || value === undefined || value === null || value === '') {
            return
        }
        if (this.state.searchValues[key] === undefined) {
            this.state.searchValues[key] = value
        }
    }

    /**
     * Updates the grid based on values of all search fields
     */
    onSearch() {
        this.grid.config.columns.forEach(column => {
            column.columns.forEach(subcolumn => {subcolumn.sort = !!column.searchable && column.searchable.sortKey !== null;})
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
     * Restores state persisted by `saveState`. Called on init and before each
     * render so filter inputs reflect what was active in the previous session.
     */
    loadState() {
        const retrievedState = JSON.parse(localStorage.getItem(`${this.dataUrl}_search`))
        if (retrievedState) {
            this.state = retrievedState
        }
    }

    /**
     * Drops every search value and returns to the grid's own default sort, wiping the persisted
     * state entirely rather than merely emptying it — so a stale key from an old grid version can't
     * resurface fields that no longer apply.
     */
    resetState() {
        localStorage.removeItem(`${this.dataUrl}_search`)
        this.state = {
            sortDirection: this.initialSortConfig.sortDirection || 'ASC',
            sortColumn: this.initialSortConfig.sortColumn || '',
            page: 0,
            limit: this.state.limit,
            searchValues: {}
        }
        this.onSearch()
    }

    /**
     * Returns the current filter values (excluding null/empty values)
     * @returns {object} Current search filter values
     */
    getFilters() {
        const filters = {};
        for (const [key, value] of Object.entries(this.state.searchValues)) {
            if (value === null || value === undefined || value === '') {
                continue
            }
            if (Array.isArray(value) && value.length === 0) {
                continue
            }
            filters[key] = value;
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
