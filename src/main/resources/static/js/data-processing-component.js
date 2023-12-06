
let dataProcessingServices = [];
let dataProcessingServicesLabelCounter = 0;
/**
 * Service that handles the "interactive" parts of the data-processing form
 */
let DataProcessingComponent = function () {
    /**
     * @param container the parent div
     * @param registeredCategories preselected information category identifiers
     * @param informationChoices1 preselected information type choices 1
     * @param informationChoices2 preselected information type choices 2
     */
    this.init = function (container, registeredCategories, informationChoices1, informationChoices2) {
        dataProcessingServices.push(this);

        this.modalContainer = container.querySelector("#modalPersonOplysninger");
        if (this.modalContainer !== undefined) {
            this.modalContainer = document.getElementById("modalPersonOplysninger");
            this.modalContainer.remove();
            document.body.appendChild(this.modalContainer);
            this.addModalListeners(this.modalContainer);
        }
        this.container = container;
        this.targetElement = container.querySelector(".categoriesTarget");
        this.registeredCategories = registeredCategories;
        this.informationChoices1 = informationChoices1;
        this.informationChoices2 = informationChoices2;
    }

    this.addModalListeners = function(modalContainer) {
        let self = this;
        modalContainer.addEventListener('show.bs.modal', function (event) {
            const modal = event.target;
            const checkBoxes = modal.querySelectorAll('input');
            for (let i = 0; i < checkBoxes.length; i++) {
                checkBoxes[i].checked = false;
            }
        });
        modalContainer.addEventListener('hide.bs.modal', function (event) {
            const modal = event.target;
            const categoryIdentifier = modal.querySelector('#categoryIdentifier').value;
            const checkBoxes = modal.querySelectorAll('input');
            const allSelect = self.container.querySelectorAll('.categorySelect');
            let selectedValues = [];
            for (let i = 0; i < checkBoxes.length; i++) {
                if (checkBoxes[i].checked) {
                    selectedValues.push(checkBoxes[i].value);
                }
            }
            let targetRow = null;
            for (let i = 0; i < allSelect.length; i++) {
                if (allSelect[i].value === categoryIdentifier) {
                    targetRow = allSelect[i].parentElement.parentElement;
                    allSelect[i].disabled = true;
                    const deleteButton = targetRow.querySelector('button');
                    deleteButton.style.display = 'block';
                    self.setPersonInformationCategories(targetRow, selectedValues);
                    self.setInformationParsedOnCategories(targetRow, null);
                }
            }
            self.addEmptyInformationCategory();
        });
    }

    this.resetCategorySelection = function() {
        this.targetElement.innerHTML = '';
        for (let i = 0; i < this.registeredCategories.length; i++) {
            this.loadInformationCategory(this.registeredCategories[i]);
        }
        this.updateCategorySelectionNamesOnForm();
    }

    this.removeCategorySelection = function (elem) {
        elem.parentElement.parentElement.remove();
        this.updateCategorySelectionNamesOnForm();
    }

    this.categorySelectionChanged = function (elem) {
        if (elem.value === "") {
            return;
        }
        const elementById = this.modalContainer.querySelector('#categoryIdentifier');
        elementById.value = elem.value;
        const modal = bootstrap.Modal.getOrCreateInstance(this.modalContainer);
        modal.show();
    }

    this.infoPassedOnSelectionChanged = function (elem) {
        const infoReceiversDiv = elem.parentElement.parentElement.querySelector(".infoReceiversDiv");
        if (elem.value === "YES") {
            infoReceiversDiv.style.display = 'block';
        } else {
            infoReceiversDiv.setAttribute('style', 'display: none !important');
        }
    }

    this.loadInformationCategory = function(category) {
        const categoryRow = this.container.querySelector('.categoryRowTemplate');
        const newRow = categoryRow.cloneNode(true);
        newRow.classList.add("categoryRow");
        newRow.classList.remove("categoryRowTemplate");
        newRow.style.display = 'block';
        let categorySelect = newRow.querySelector('.categorySelect');
        categorySelect.value = category.personCategoriesRegisteredIdentifier;
        categorySelect.disabled = true;
        this.setPersonInformationCategories(newRow, category.personCategoriesInformationIdentifiers);
        this.setInformationParsedOnCategories(newRow, category);
        this.targetElement.appendChild(newRow);
    }

    this.setPersonInformationCategories = function (categoryRow, tags) {
        const tagWrapper = categoryRow.querySelector('.tagin-wrapper');
        let selectedPersonInformation = categoryRow.querySelector('.selectedPersonInformation');
        let selectedRegisteredCategory = categoryRow.querySelector('.selectedRegisteredCategory');

        tagWrapper.innerHTML = '';
        for (let i = 0; i < tags.length; i++) {
            const tagDiv = document.createElement("span");
            tagDiv.classList = ['tagin-tag'];
            tagDiv.innerText = this.lookupInformationChoiceValue(tags[i]);
            tagWrapper.appendChild(tagDiv);
        }
        selectedPersonInformation.value = tags.join(',');
        selectedRegisteredCategory.value = categoryRow.querySelector('.categorySelect').value;
    }

    this.setInformationParsedOnCategories = function (categoryRow, category) {
        const selector = categoryRow.querySelector(".infoPassedOnSelect");
        const extendedDiv = categoryRow.querySelector(".extendedCategoryInfo");
        const infoReceiversDiv = categoryRow.querySelector(".infoReceiversDiv");
        const receiverBoxes = infoReceiversDiv.querySelectorAll("input[type='checkbox']");
        const parsedOn = category ? category.informationPassedOn : null;
        const receivers = category ? category.informationReceivers : null;

        extendedDiv.style.display = 'block';
        selector.value = parsedOn || "NO";
        if (parsedOn === "YES") {
            infoReceiversDiv.style.display = 'block';
        }
        if (receivers != null) {
            for (let i = 0; i < receivers.length; i++) {
                const rec = receivers[i];
                for (let j = 0; j < receiverBoxes.length; j++) {
                    if (receiverBoxes[j].value === rec) {
                        receiverBoxes[j].checked = true;
                    }
                }
            }
        }
    }

    this.addEmptyInformationCategory = function() {
        const categoryRow = this.container.querySelector('.categoryRowTemplate');
        const newRow = categoryRow.cloneNode(true);
        const selects = newRow.querySelector('.categorySelect');
        let removeButton = newRow.querySelector('button');
        selects.disabled = false;
        newRow.style.display = 'block';
        removeButton.setAttribute('style', 'display: none !important');
        newRow.classList.add("categoryRow");
        newRow.classList.remove("categoryRowTemplate");
        this.setPersonInformationCategories(newRow, ['Vælg kategori'])
        this.targetElement.appendChild(newRow);
        this.updateCategorySelectionNamesOnForm();
    }

    this.removeEmptyInformationCategories = function () {
        let rows = this.container.querySelectorAll('.categoryRow');
        for (let i = 0; i < rows.length; i++) {
            let select = rows[i].querySelector('.categorySelect');
            if (select.value === "") {
                rows[i].remove();
            }
        }
    }

    this.updateCategorySelectionNamesOnForm = function () {
        let rows = this.container.querySelectorAll('.categoryRow') || [];
        for (let i = 0; i < rows.length; i++) {
            const selectedPersonInformation = rows[i].querySelector('.selectedPersonInformation');
            const selectedRegisteredCategory = rows[i].querySelector('.selectedRegisteredCategory');
            const infoPassedOnSelect = rows[i].querySelector('.infoPassedOnSelect');
            const infoReceiversDiv = rows[i].querySelector('.infoReceiversDiv');
            const infoReceiversInputs = infoReceiversDiv.querySelectorAll("input");
            const infoReceiversLabels = infoReceiversDiv.querySelectorAll("label");
            selectedRegisteredCategory.setAttribute('name', `personCategoriesRegistered[${i}].personCategoriesRegisteredIdentifier`);
            selectedPersonInformation.setAttribute('name', `personCategoriesRegistered[${i}].personCategoriesInformationIdentifiers`);
            infoPassedOnSelect.setAttribute('name', `personCategoriesRegistered[${i}].informationPassedOn`);
            for (let j = 0; j < infoReceiversInputs.length; j++) {
                infoReceiversInputs[j].setAttribute('name', `personCategoriesRegistered[${i}].informationReceivers`);
                infoReceiversInputs[j].setAttribute('id', `infoRecChk${dataProcessingServicesLabelCounter}`);
                infoReceiversLabels[j].setAttribute('for', `infoRecChk${dataProcessingServicesLabelCounter}`);
                dataProcessingServicesLabelCounter++;
            }
        }
    }

    this.lookupInformationChoiceValue = function (choiceIdentifier) {
        for (let element of this.informationChoices1) {
            if (element.identifier === choiceIdentifier) {
                return element.caption;
            }
        }
        for (let element of this.informationChoices2) {
            if (element.identifier === choiceIdentifier) {
                return element.caption;
            }
        }
        return choiceIdentifier;
    }

    this.editModeRemoveCategoryButtons = function(enabled) {
        let rows = this.container.querySelectorAll('.categoryRow');
        for (let i = 0; i < rows.length; i++) {
            let select = rows[i].querySelector('.categorySelect');
            let button = rows[i].querySelector('button');
            if (select.value !== "") {
                button.style.display = enabled ? 'block' : 'none !important';
            }
        }
    }

    this.editModeCategoryInformationEditable = function (enabled) {
        if (enabled) {
            this.editModeRemoveCategoryButtons(enabled);
            this.addEmptyInformationCategory();
        } else {
            this.editModeRemoveCategoryButtons(enabled);
            this.removeEmptyInformationCategories();
            this.resetCategorySelection();
        }
    }

}
