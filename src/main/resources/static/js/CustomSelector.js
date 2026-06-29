export default class CustomMultiSelect {
    #noSelectedOptionText = 'Intet filter';
    #selectedPostFixText = 'valgt';
    #select;
    #button;
    #dropdown;
    #isOpen = false;
    #tempSelections = new Set();

    #handleOutsideClick = (e) => {
        if (this.#isOpen && !this.#button.contains(e.target) && !this.#dropdown.contains(e.target)) {
            this.#closeDropdown();
        }
    };

    #reposition = () => this.#positionDropdown();

    constructor(container, selectElement) {
        if (!container || !selectElement?.hasAttribute('multiple')) {
            throw new Error('Container element and a multiple select element are required');
        }

        this.#select = selectElement;

        this.#button = document.createElement('button');
        this.#button.type = 'button';
        this.#button.className = 'custom-select-button';
        this.#button.textContent = this.#getButtonText();
        this.#button.addEventListener('click', (e) => {
            e.stopPropagation();
            this.#isOpen ? this.#closeDropdown() : this.#openDropdown();
        });
        container.appendChild(this.#button);

        this.#dropdown = document.createElement('div');
        this.#dropdown.className = 'custom-select-dropdown';
        this.#dropdown.style.display = 'none';
        for (const option of selectElement.options) {
            const item = document.createElement('div');
            item.className = 'custom-select-item';
            item.textContent = option.text;
            item.dataset.value = option.value;
            item.addEventListener('click', (e) => {
                e.stopPropagation();
                this.#toggleItem(item);
            });
            this.#dropdown.appendChild(item);
        }
        document.body.appendChild(this.#dropdown);

        document.addEventListener('click', this.#handleOutsideClick);
    }

    #openDropdown() {
        this.#tempSelections = new Set(
            Array.from(this.#select.selectedOptions).map(opt => opt.value)
        );
        for (const item of this.#dropdown.querySelectorAll('.custom-select-item')) {
            item.classList.toggle('selected', this.#tempSelections.has(item.dataset.value));
        }
        this.#dropdown.style.display = 'block';
        this.#isOpen = true;
        this.#button.classList.add('open');
        this.#positionDropdown();
        window.addEventListener('scroll', this.#reposition, true);
        window.addEventListener('resize', this.#reposition);
    }

    #closeDropdown() {
        window.removeEventListener('scroll', this.#reposition, true);
        window.removeEventListener('resize', this.#reposition);
        this.#dropdown.style.display = 'none';
        this.#isOpen = false;
        this.#button.classList.remove('open');
        this.#commitSelections();
    }

    #toggleItem(item) {
        const value = item.dataset.value;
        if (this.#tempSelections.has(value)) {
            this.#tempSelections.delete(value);
            item.classList.remove('selected');
        } else {
            this.#tempSelections.add(value);
            item.classList.add('selected');
        }
    }

    #commitSelections() {
        let changed = false;
        for (const option of this.#select.options) {
            const shouldBeSelected = this.#tempSelections.has(option.value);
            if (option.selected !== shouldBeSelected) {
                option.selected = shouldBeSelected;
                changed = true;
            }
        }
        if (changed) {
            this.#button.textContent = this.#getButtonText();
            this.#select.dispatchEvent(new Event('change', { bubbles: true }));
        }
    }

    #positionDropdown() {
        const rect = this.#button.getBoundingClientRect();
        const spaceBelow = window.innerHeight - rect.bottom;
        const openAbove = spaceBelow < this.#dropdown.offsetHeight && rect.top > spaceBelow;
        this.#dropdown.style.top = openAbove
            ? `${rect.top - this.#dropdown.offsetHeight - 4}px`
            : `${rect.bottom + 4}px`;
        this.#dropdown.style.left = `${rect.left}px`;
        this.#dropdown.style.minWidth = `${rect.width}px`;
    }

    #getButtonText() {
        const selected = Array.from(this.#select.selectedOptions);
        if (selected.length === 0) {
            return this.#noSelectedOptionText;
        }
        if (selected.length === 1) {
            return selected[0].text;
        }
        return `${selected.length} ${this.#selectedPostFixText}`;
    }

    destroy() {
        document.removeEventListener('click', this.#handleOutsideClick);
        this.#button.remove();
        this.#dropdown.remove();
    }
}
