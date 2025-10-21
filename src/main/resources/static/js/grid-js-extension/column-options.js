export default class ColumnOptions {
    tableElementId
    neverShowIds = []
    alwaysShowIds = []
    grid = null;
    state = {}
    localStorageKey = `${window.location.pathname}-column-options`;
    itemTemplate = null // TODO
    optionsContainer = null;   // TODO

    constructor(tableElementId, grid, alwaysShowIds = [], neverShowIds = ['id'], defaultShowingIds = []) {
        this.neverShowIds = neverShowIds;
        this.alwaysShowIds = alwaysShowIds;
        this.getInitialState(defaultShowingIds);
        this.itemTemplate = document.getElementById('column-options-item-template');
        this.tableElementId = tableElementId
    }

    getInitialState(defaultShowingIds) {
        const localState = this.loadLocally()
        const columns = this.grid.columns
        for (let column of columns) {
            // calculate initial state. Default is false if not otherwise indicated
            const defaultShowing = defaultShowingIds.includes(column.id)
            const locallyStoredShowing = localState[column.id]
            const alwaysShow = this.alwaysShowIds.includes(column.id)
            const neverShow = this.neverShowIds.includes(column.id)

            // do not show if this is marked as neverShow, otherwise look at alwaysShow, then locally stored value, then defaults
            const shouldShow = neverShow ? false : alwaysShow || locallyStoredShowing || defaultShowing

            // populate state by objects representing the columns, indexed by id
            this.state[column.id] = {
                id: column.id, name: column.name, hidden: shouldShow, // default setting is false if not explicitly otherwise
                alwaysShow: alwaysShow, neverShow: neverShow,
            };
        }
    }

    setHidden(id, hidden) {
        const state = this.state[id];
        if (state && !state.alwaysShow) {
            state.hidden = hidden;
        }
    }

    updateGrid() {


        this.grid.forceRender();
    }

    saveLocally() {
        // Save minimum
        const minimalState = {}
        for (let [id, state] of Object.entries(this.state)) {
            minimalState[id] = state.hidden
        }

        // stringify
        const json = JSON.stringify(minimalState)

        //save
        localStorage.setItem(this.localStorageKey, json)
    }

    loadLocally() {
        // load
        const savedStateString = localStorage.getItem(this.localStorageKey);

        // parse
        if (savedStateString) {
            return JSON.parse(savedStateString);
        }
    }

    createColumnOptions() {
        // reset existing
        this.optionsContainer.innerHTML = '';

        // create item for all that can possibly be shown
        for (const column of this.state) {
            // do not show columns that cannot be changed
            if (!column.neverShow && !column.alwaysShow) {
                const item = this.createItem()
                if (item) {
                    this.optionsContainer.appendChild(item)
                }
            }
        }

    }

    createItem(id, label, hidden) {
        if (this.itemTemplate) {
            const clone = this.itemTemplate.content.cloneNode(true);

            clone.dataset.columnId = id;
            const labelElement = clone.querySelector('.columnName')
            if (labelElement) {
                labelElement.textContent = label
            }

            return clone
        }
        return null;
    }

    toggleOption(element) {
        if (element) {
            const iconElement = element.querySelector('.option-icon')
            iconElement.classList.toggle('ti-check');
            iconElement.classList.toggle('ti-minus');
        }
    }

    createOptionsContainer() {
        const optionsContainerTemplate = document.getElementById('column-options-container-template');
        const clone = optionsContainerTemplate.content.cloneNode(true);

        const datatableElement = document.getElementById(this.tableElementId);
        datatableElement.before(clone);
    }

}
