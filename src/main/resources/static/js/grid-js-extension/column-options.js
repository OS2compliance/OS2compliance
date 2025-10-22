/**
 * Creates an option UI for a GridJS datatable, which allows columns to be hidden and shown based on user preference
 */
export default class ColumnOptions {
    tableElementId
    neverShowIds = []
    alwaysShowIds = []
    grid = null;
    state = {}
    localStorageKey = `${window.location.pathname}/column-options`;
    itemTemplate = null
    optionsContainer = null;
    tempState
    buttonContainerSelector = '.tableOptionsContainer'

    optionContainerTemplateId = 'columnOptionContainerTemplate'
    optionItemTemplateId = 'columnOptionItemTemplate'
    optionIconClass = 'optionIcon';
    optionLabelClass = 'optionLabel';
    optionItemClass = 'optionItem';
    optionsMenuContainerClass = 'columnOptionsMenuContainer'
    toggleOptionsButtonClass = 'toggleColumnOptionsButton';
    confirmButtonClass = 'columnOptionsConfirmButton';

    /**
     * Creates a GridColumn instantiation for the given grid, with the configs provided
     * @param tableElementId id of the container for the table
     * @param grid the GridJS instance of the table
     * @param alwaysShowIds a list of column Id's for columns that should always be shown (and thus not customizable by the user)
     * @param defaultShowingIds a list of column Ids for columns that should be shown by default
     * @param neverShowIds a list of column Id's for columns that should always be hidden for the user
     * @param buttonContainerSelector An optional selector for the container which houses the option toggle.
     * If the container is not found, the toggle is placed right before the table container
     */
    constructor(tableElementId, grid, alwaysShowIds = [], defaultShowingIds = [], neverShowIds = ['id'], buttonContainerSelector = '.tableOptionsContainer') {
        if (!tableElementId || !grid) {
            throw new Error('ColumnOptions was not provided with required arguments');
        }

        this.grid = grid;
        this.neverShowIds = neverShowIds;
        this.alwaysShowIds = alwaysShowIds;
        this.itemTemplate = document.getElementById(this.optionItemTemplateId);
        this.tableElementId = tableElementId
        this.buttonContainerSelector = buttonContainerSelector

        this.getInitialState(defaultShowingIds);
        this.createOptionsContainer()
        this.updateGrid()
    }

    /**
     * Creates the initial state for the columns
     * @param defaultShowingIds
     */
    getInitialState(defaultShowingIds) {
        const localState = this.loadLocally()
        const columns = this.grid.config.columns
        for (let column of columns) {
            // calculate initial state. Default is false if not otherwise indicated
            const defaultShowing = defaultShowingIds.includes(column.id)


            const hasStoredValue = localState?.hasOwnProperty(column.id)
            const locallyStoredHidden = hasStoredValue ? localState[column.id] : null


            const alwaysShow = this.alwaysShowIds.includes(column.id)
            const neverShow = this.neverShowIds.includes(column.id)

            // do not show if this is marked as neverShow, otherwise look at alwaysShow, then locally stored value, then defaults
            let shouldShow;
            if (neverShow) {
                shouldShow = false;
            } else if (alwaysShow) {
                shouldShow = true;
            } else if (hasStoredValue) {
                shouldShow = !locallyStoredHidden;
            } else {
                shouldShow = defaultShowing;
            }

            // populate state by objects representing the columns, indexed by id
            this.state[column.id] = {
                id: column.id,
                name: column.name,
                hidden: !shouldShow, // default setting is false if not explicitly otherwise
                alwaysShow: alwaysShow,
                neverShow: neverShow,
            };
        }
    }

    /**
     * Updates and re-renders the grid
     */
    updateGrid() {
        // modify grid to match current state
        const columns = this.grid.config.columns
        for (let column of columns) {
            const hiddenState = this.state[column.id]
            column.hidden = hiddenState.hidden

            // If custom search functionality is used, trigger the hidden update
            if (column.onHiddenUpdate) {
                column.onHiddenUpdate()
            }
        }

        // force a re-render
        this.grid.forceRender();
    }

    /**
     * Saves the current state (simplified) to local storage as json
     */
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

    /**
     * Loads the simplified column state from localstorage and parses it
     * @returns {any}
     */
    loadLocally() {
        // load
        const savedStateString = localStorage.getItem(this.localStorageKey);

        // parse
        if (savedStateString) {
            return JSON.parse(savedStateString);
        }
    }

    /**
     * Populates the option container with options
     */
    createColumnOptions() {

        const optionMenuContainer = this.optionsContainer.querySelector(`.${this.optionsMenuContainerClass}`);

        // reset existing
        optionMenuContainer.innerHTML = '';
        this.tempState = {}

        // create item for all that can possibly be shown
        for (const [id, column] of Object.entries(this.state)) {
            // do not show columns that cannot be changed
            if (!column.neverShow && !column.alwaysShow) {
                const item = this.createItem(id, column.name, !column.hidden);
                if (item) {
                    this.tempState[column.id] = column.hidden;
                    optionMenuContainer.appendChild(item)
                }
            }
        }

    }

    /**
     * Creates an option html object
     * @param id column id for the option item
     * @param label label to show for the option
     * @param asColumnShown if true, the option is created for a shown column. Otherwise it represents a hidden column by default.
     * @returns {Element|null} an option, or null if no template for the item was found
     */
    createItem(id, label, asColumnShown) {
        if (this.itemTemplate) {
            const clone = this.itemTemplate.content.cloneNode(true).firstElementChild;

            clone.dataset.columnId = id;
            const labelElement = clone.querySelector(`.${this.optionLabelClass}`)
            if (labelElement) {
                labelElement.textContent = label
            }

            const iconElement = clone.querySelector(`.${this.optionIconClass}`)
            if (iconElement && asColumnShown) {
                iconElement.classList.add('ti-check')
                iconElement.classList.remove('ti-minus')
            }

            return clone
        } else {
            console.error("no template found for column option item")
        }
        return null;
    }

    /**
     * Toggles the given option element between the shown and hidden state, pushing the new state to the temporary state holder
     * @param element an option element
     */
    toggleOption(element) {
        if (element) {
            const iconElement = element.querySelector(`.${this.optionIconClass}`)
            const id = element.dataset.columnId
            const isCurrentlyShown = !this.state[id].hidden

            if (isCurrentlyShown) {
                iconElement.classList.remove('ti-check')
                iconElement.classList.add('ti-minus')
                this.tempState[id] = true
            } else {
                iconElement.classList.add('ti-check')
                iconElement.classList.remove('ti-minus')
                this.tempState[id] = false
            }
        } else {
            console.info("Attempted to toggle option, but no element was passed")
        }
    }

    /**
     * Creates and injects the option container into the document
     */
    createOptionsContainer() {
        const optionsContainerTemplate = document.getElementById(this.optionContainerTemplateId);
        if (optionsContainerTemplate) {
            const clone = optionsContainerTemplate.content.cloneNode(true);

            this.optionsContainer = clone.firstElementChild;

            const datatableElement = document.getElementById(this.tableElementId);

            const existingTableOptionsContainer = document.querySelector(this.buttonContainerSelector);
            if (existingTableOptionsContainer) {
                existingTableOptionsContainer.prepend(clone)
            } else {
                datatableElement.before(clone);
            }

            this.initOptions(this.optionsContainer);
        } else {
            console.error("No template for column options found.")
        }
    }

    /**
     * Initializes functionality for the option container0
     * @param optionsContainer optionscontainer
     */
    initOptions(optionsContainer) {
        if (!optionsContainer) {
            console.error("Could not initiate column options. No container was passed")
        }

        // fill with options
        this.createColumnOptions()

        // add event listener for options
        optionsContainer.addEventListener('click', (e) => {
            const optionItem = e.target.closest(`.${this.optionItemClass}`);
            if (!optionItem) {
                return;
            }

            e.stopPropagation();
            e.preventDefault();
            this.toggleOption(optionItem);
        })

        // add event listener for confirm button
        const confirmButton = optionsContainer.querySelector(`.${this.confirmButtonClass}`)
        if (confirmButton) {
            confirmButton.addEventListener('click', () => {
                this.updateStateFromTempState()
                this.saveLocally()
                this.updateGrid()
            })
        }

        // ensure list options are generated on opening dropdown
        const toggleOptionsButton = optionsContainer.querySelector(`.${this.toggleOptionsButtonClass}`)
        if (toggleOptionsButton) {
            toggleOptionsButton.addEventListener('click', () => this.createColumnOptions())
        }
    }

    /**
     * Pushes the temporary state to the actual permanent state
     */
    updateStateFromTempState() {
        for (let [id, state] of Object.entries(this.tempState)) {
            this.state[id].hidden = state
        }
    }

}
