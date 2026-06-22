
export default class CustomMultiSelect {
    noSelectedOptionText = 'Intet filter'
    selectedPostFixText = 'valgt'

    constructor(containerElement, selectElement) {
        if (!containerElement || !selectElement) {
            throw new Error('Container and select elements are required');
        }

        if (!selectElement.hasAttribute('multiple')) {
            throw new Error('Select element must have multiple attribute');
        }

        this.container = containerElement;
        this.originalSelect = selectElement;
        this.isOpen = false;
        this.tempSelections = new Set();

        // Generate unique ID for this instance
        this.instanceId = `custom-select-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

        this.init();
    }

    init() {
        // Create button
        this.button = document.createElement('button');
        this.button.type = 'button';
        this.button.className = 'custom-select-button';
        this.button.textContent = this.getButtonText();
        this.button.dataset.instance = this.instanceId;

        // Create dropdown container - append to body instead of container
        this.dropdown = document.createElement('div');
        this.dropdown.className = 'custom-select-dropdown';
        this.dropdown.style.display = 'none';
        this.dropdown.dataset.instance = this.instanceId;

        // Build options list
        this.buildOptionsList();

        // Append button to container
        this.container.appendChild(this.button);

        // Append dropdown to body (portal pattern)
        document.body.appendChild(this.dropdown);

        // Bind events
        this.button.addEventListener('click', (e) => this.toggleDropdown(e));

        // Close on outside click
        this.outsideClickHandler = (e) => this.handleOutsideClick(e);
        document.addEventListener('click', this.outsideClickHandler);

        // Reposition on scroll/resize
        this.repositionHandler = () => this.positionDropdown();
        window.addEventListener('scroll', this.repositionHandler, true);
        window.addEventListener('resize', this.repositionHandler);
    }

    buildOptionsList() {
        this.dropdown.innerHTML = '';
        const options = Array.from(this.originalSelect.options);

        options.forEach((option) => {
            const item = document.createElement('div');
            item.className = 'custom-select-item';
            item.textContent = option.text;
            item.dataset.value = option.value;

            if (option.selected) {
                item.classList.add('selected');
            }

            item.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleOption(option.value, item);
            });

            this.dropdown.appendChild(item);
        });
    }

    toggleOption(value, itemElement) {
        if (this.tempSelections.has(value)) {
            this.tempSelections.delete(value);
            itemElement.classList.remove('selected');
        } else {
            this.tempSelections.add(value);
            itemElement.classList.add('selected');
        }
    }

    toggleDropdown(e) {
        e.stopPropagation();

        if (this.isOpen) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    positionDropdown() {
        if (!this.isOpen) return;

        const buttonRect = this.button.getBoundingClientRect();
        const dropdownHeight = this.dropdown.offsetHeight;
        const viewportHeight = window.innerHeight;
        const spaceBelow = viewportHeight - buttonRect.bottom;
        const spaceAbove = buttonRect.top;

        // Determine if dropdown should open above or below
        const openAbove = spaceBelow < dropdownHeight && spaceAbove > spaceBelow;

        if (openAbove) {
            // Position above button
            this.dropdown.style.top = `${buttonRect.top + window.scrollY - dropdownHeight - 4}px`;
        } else {
            // Position below button
            this.dropdown.style.top = `${buttonRect.bottom + window.scrollY + 4}px`;
        }

        this.dropdown.style.left = `${buttonRect.left + window.scrollX}px`;
        this.dropdown.style.minWidth = `${buttonRect.width}px`;
    }

    openDropdown() {
        // Initialize temp selections with current select values
        this.tempSelections = new Set(
            Array.from(this.originalSelect.selectedOptions).map(opt => opt.value)
        );

        // Update visual state of items
        const items = this.dropdown.querySelectorAll('.custom-select-item');
        for (let item of items) {
            const value = item.dataset.value
            if (this.tempSelections.has(value)) {
                item.classList.add('selected');
            } else {
                item.classList.remove('selected');
            }

        }

        this.dropdown.style.display = 'block';
        this.isOpen = true;
        this.button.classList.add('open');

        // Position the dropdown
        this.positionDropdown();
    }

    closeDropdown() {
        this.dropdown.style.display = 'none';
        this.isOpen = false;
        this.button.classList.remove('open');

        // Update original select
        this.updateOriginalSelect();
    }

    updateOriginalSelect() {
        let changed = false;

        Array.from(this.originalSelect.options).forEach(option => {
            const shouldBeSelected = this.tempSelections.has(option.value);
            if (option.selected !== shouldBeSelected) {
                option.selected = shouldBeSelected;
                changed = true;
            }
        });

        // Emit change event if selections changed
        if (changed) {
            this.button.textContent = this.getButtonText();
            const event = new Event('change', { bubbles: true });
            this.originalSelect.dispatchEvent(event);
        }
    }

    getButtonText() {
        const selectedOptions = Array.from(this.originalSelect.selectedOptions);

        if (selectedOptions.length === 0) {
            return this.noSelectedOptionText;
        } else if (selectedOptions.length === 1) {
            return selectedOptions[0].text;
        } else {
            return `${selectedOptions.length} ${this.selectedPostFixText}`;
        }
    }

    handleOutsideClick(e) {
        if (this.isOpen) {
            const isClickInside = this.button.contains(e.target) || this.dropdown.contains(e.target);
            if (!isClickInside) {
                this.closeDropdown();
            }
        }
    }

    destroy() {
        // Remove event listeners
        document.removeEventListener('click', this.outsideClickHandler);
        window.removeEventListener('scroll', this.repositionHandler, true);
        window.removeEventListener('resize', this.repositionHandler);

        // Remove created elements
        this.button?.remove();
        this.dropdown?.remove();

        // Show original select
        this.originalSelect.style.display = '';
    }

    refresh() {
        // Rebuild options list from current select state
        this.buildOptionsList();
        this.button.textContent = this.getButtonText();
    }
}
