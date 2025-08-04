const CATEGORYMARKER = 'currentCategorySelector'
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

        this.container = container;
        this.modalContainer = container.querySelector("#modalPersonOplysninger");
        if (this.modalContainer !== undefined) {
            this.modalContainer = document.getElementById("modalPersonOplysninger");
            this.modalContainer.remove();
            document.body.appendChild(this.modalContainer);
            this.addModalListeners(this.modalContainer);
        }
        this.targetElement = container.querySelector(".categoriesTarget");
        this.registeredCategories = registeredCategories;
        this.informationChoices1 = informationChoices1;
        this.informationChoices2 = informationChoices2;

        this.initInfoPassedOnFunctionality()
    }

    this.addModalListeners = function (modalContainer) {
        let self = this;
        modalContainer.addEventListener('hide.bs.modal', function (event) {
            const modal = event.target;
            const checkBoxes = modal.querySelectorAll('input');
            let selectedValues = [...checkBoxes]
                .filter(checkbox => checkbox.checked)
                .map(checkbox => checkbox.value);
            let targetRow = null;

            const markedSelects = document.getElementsByClassName(CATEGORYMARKER)
            if (markedSelects.length > 0) {
                const select = document.getElementsByClassName(CATEGORYMARKER)[0]

                // Remove marker for the category to ensure only one select is ever marked
                select.classList.remove(CATEGORYMARKER);

                targetRow = select.parentElement.parentElement;
                const deleteButton = targetRow.querySelector('button');
                deleteButton.style.display = 'block';
            }
            self.setPersonInformationCategories(targetRow, selectedValues);
            self.setInformationParsedOnCategories(targetRow, null);

            self.removeEmptyInformationCategories();
            self.addEmptyInformationCategory();
        });
    }

    this.resetCategorySelection = function() {
        this.targetElement.innerHTML = '';
        for (const registeredCategory of this.registeredCategories) {
            this.loadInformationCategory(registeredCategory);
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
        let container = elem.parentElement.parentElement
        container.querySelector('.tagin-wrapper').classList.remove('disabledBox');
        let tagsValues = [];
        let selectedTags = container.querySelectorAll('.tagin-tag');
        for (const selectedTag of selectedTags) {
            tagsValues.push(selectedTag.textContent);
        }
        this.setPersonInformationCategories(container, tagsValues);
    }

    this.preselectValuesInModal = function(containerElem) {
        const checkBoxes = this.modalContainer.querySelectorAll('input');
        for (const checkBox of checkBoxes) {
            checkBox.checked = false;
        }
        let selectedTags = containerElem.querySelectorAll('.tagin-tag');
        for (const selectedTag of selectedTags) {
            let element = this.modalContainer.querySelector(`input[id^='${selectedTag.dataset.id}']`);
            if (element != null) {
                element.checked = true;
            }
        }
    }

    this.informationTypesClicked = function(elem) {
        if (!elem.classList.contains('disabledBox')) {
            let container = elem.parentElement.parentElement
            let categorySelectorElem = container.querySelector(".categorySelect");
            const elementById = this.modalContainer.querySelector('#categoryIdentifier');
            elementById.value = categorySelectorElem.value;

            this.preselectValuesInModal(elem);

            // Mark category selector with a class, to make it easy to find it once modal closes.
            categorySelectorElem.classList.add(CATEGORYMARKER)

            const modal = bootstrap.Modal.getOrCreateInstance(this.modalContainer);
            modal.show();
        }
    }

    this.infoPassedOnSelectionChanged = function (elem) {
        const parent = elem.closest('.extendedCategoryInfo')

        this.showInfoRecieversElementFor(parent, elem?.value === "YES")
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
        for (const tag of tags) {
            const tagDiv = document.createElement("span");
            tagDiv.dataset.id = tag;
            tagDiv.classList = ['tagin-tag'];
            tagDiv.innerText = this.lookupInformationChoiceValue(tag);
            tagWrapper.appendChild(tagDiv);
        }
        tagWrapper.innerHTML += '&nbsp';
        selectedPersonInformation.value = tags.join(',');
        selectedRegisteredCategory.value = categoryRow.querySelector('.categorySelect').value;
    }

    this.setInformationParsedOnCategories = function (categoryRow, category) {
        const selector = categoryRow.querySelector(".infoPassedOnSelect");
        const extendedDiv = categoryRow.querySelector(".extendedCategoryInfo");
        const infoReceiversDiv = categoryRow.querySelector(".infoReceiversDiv");
        const receiverSelectedBoxes = infoReceiversDiv.querySelectorAll("input[type='checkbox'].receiverSelectedCheck");
        const passedOn = category ? category.informationPassedOn : null;
        const infoReceivers = category ? category.informationReceivers : null;

        extendedDiv.hidden = false;
        selector.value = passedOn || "NO";
        if (passedOn === "YES") {
            this.showInfoRecieversElementFor(categoryRow ,true)
            this.setStateForPassedOnComment(categoryRow, category)
        }
        if (infoReceivers != null) {
            this.setStateForInfoReceivers(infoReceivers, receiverSelectedBoxes);
        }
    }

    this.setStateForPassedOnComment = function (categoryRow, category) {
        const commentTextfield = categoryRow.querySelector(".infoPassedOnComment");
        if (commentTextfield) {
            commentTextfield.value = category.receiverComment || "";
        }
    }

    this.setStateForInfoReceivers = function (infoReceivers, receiverSelectedBoxes) {
        // For each infoReceiver object in all categories
        for (const infoReceiver of infoReceivers) {
            // For each individual receiver
            for (const receiverSelectedCheckbox of receiverSelectedBoxes) {
                // If the box identifier is one of the selected identifiers...
                if (receiverSelectedCheckbox.value === infoReceiver.choiceValue.identifier) {
                    const infoReceiverContainer = receiverSelectedCheckbox.closest('.infoReceiverContainer');

                    // Check the box for this receiver
                    receiverSelectedCheckbox.checked = true;

                    // Show accompanying options
                    const additionalOptionsContainer = infoReceiverContainer.querySelector('.passedOnAdditionalOptionsContainer')
                    additionalOptionsContainer.hidden = false;

                    // Check the radiobutton for selected location, default to 'inside EU'
                    this.checkSelectedRadioButton(infoReceiver.receiverLocation, additionalOptionsContainer)
                }
            }
        }
    }


    this.checkSelectedRadioButton = function (receiverLocation, additionalOptionsContainer) {
        if (receiverLocation === 'OUTSIDE_EU') {
            const outsideEURadioButton = additionalOptionsContainer.querySelector('.outsideEURadio')
            if (outsideEURadioButton) {
                outsideEURadioButton.checked = true;
            }
        } else {
            const insideEuRadioButton = additionalOptionsContainer.querySelector('.insideEURadio')
            if (insideEuRadioButton) {
                insideEuRadioButton.checked = true;
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
        newRow.querySelector('.tagin-wrapper').classList.add('disabledBox');
        this.setPersonInformationCategories(newRow, ['Vælg type'])
        this.targetElement.appendChild(newRow);
        this.updateCategorySelectionNamesOnForm();
    }

    this.removeEmptyInformationCategories = function () {
        let rows = this.container.querySelectorAll('.categoryRow');
        for (const row of rows) {
            let select = row.querySelector('.categorySelect');
            if (select.value === "") {
                row.remove();
            }
        }
    }

    this.updateCategorySelectionNamesOnForm = function () {
        let rows = this.container.querySelectorAll('.categoryRow') || [];

        for (let i = 0; i < rows.length; i++) {
            const baseName = `personCategoriesRegistered[${i}]`
            const categoryRow = rows[i];

            const selectedPersonInformation = categoryRow?.querySelector('.selectedPersonInformation');
            const selectedRegisteredCategory = categoryRow?.querySelector('.selectedRegisteredCategory');
            const infoPassedOnSelect = categoryRow?.querySelector('.infoPassedOnSelect');

            selectedRegisteredCategory?.setAttribute('name', `${baseName}.personCategoriesRegisteredIdentifier`);
            selectedPersonInformation?.setAttribute('name', `${baseName}.personCategoriesInformationIdentifiers`);
            infoPassedOnSelect?.setAttribute('name', `${baseName}.informationPassedOn`);

            this.updateNamesForInfoReceiversControls(categoryRow, baseName)
            this.updateNameForInfoPassedOnComment(categoryRow, baseName)
        }
    }

    this.updateNameForInfoPassedOnComment = function (categoryRow, categoryBaseName) {
        // Set name and id for comment textbox
        const commentInput = categoryRow.querySelector('.infoPassedOnComment');
        const commentLabel = commentInput.parentElement.querySelector('label');
            const id = `${categoryBaseName}_passedon_comment`
        commentInput.id = id
        commentLabel.setAttribute('for', `${id}`)
        commentInput.setAttribute('name', `${categoryBaseName}.informationPassedOnComment`);
    }

    this.updateNamesForInfoReceiversControls = function (categoryRow, categoryBaseName) {
        const infoReceiverContainers =  categoryRow.querySelectorAll('.infoReceiverContainer');
        for (let i = 0; i < infoReceiverContainers.length; i++) {
            const receiverContainer = infoReceiverContainers[i]
            const receiverContainerBaseName = `informationReceivers[${i}]`

            // Set id, and name for the main receiver checkbox
            const receiverSelectedInput = receiverContainer.querySelector('input.receiverSelectedCheck');
            receiverSelectedInput.setAttribute('name', `${categoryBaseName}.${receiverContainerBaseName}.choiceValueIdentifier`);
            const id = `receiverLocationRadio_${dataProcessingServicesLabelCounter++}`
            receiverSelectedInput.id = id
            const receiverSelectedLabel = receiverSelectedInput.nextElementSibling;
            receiverSelectedLabel.setAttribute('for', id)

            // Set name and id for each location radiobutton
            const locationRadioButtons = receiverContainer.querySelectorAll('input.locationRadio');
            for (const locationRadioButton of locationRadioButtons) {
                locationRadioButton.setAttribute('name', `${categoryBaseName}.${receiverContainerBaseName}.receiverLocation`);
                const locationId = `receiverLocationRadio_${dataProcessingServicesLabelCounter++}`
                locationRadioButton.id = locationId;
                const radioLabel = locationRadioButton.nextElementSibling;
                radioLabel.setAttribute('for', locationId);
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
        for (const element of rows) {
            let select = element.querySelector('.categorySelect');
            let button = element.querySelector('button');
            if (select.value !== "") {
                button.style.display = enabled ? 'block' : 'none !important';
            }
        }
    }

    this.setEditMode = function (enabled) {
        // show/hide buttons
        this.editModeRemoveCategoryButtons(enabled);
        // enable/disable category select
        const allSelect = this.container.querySelectorAll('.categorySelect');
        for (const select of allSelect) {
            select.disabled = !enabled;
        }
        // enable/disable types tag box
        const allTypesBoxes = this.container.querySelectorAll('.tagin-wrapper');
        for (const box of allTypesBoxes) {
            if (enabled) {
                box.classList.remove("disabledBox");
            } else {
                ensureElementHasClass(box, "disabledBox");
            }
        }

        if (enabled) {
            this.addEmptyInformationCategory();
        } else {
            this.removeEmptyInformationCategories();
            this.resetCategorySelection();
        }
    }

    this.initInfoPassedOnFunctionality = function () {
        // Add event delegation for info passed on select
        const tableElement = document.querySelector('table.dataprocessingTable');
        tableElement?.addEventListener('change', e => {
            const target = e.target;
            if (target?.classList.contains('infoPassedOnSelect')) {
                this.infoPassedOnSelectionChanged(target);
            }
        })

        // Add event delegation for showing receiver location radio buttons
        tableElement?.addEventListener('change', e => {
            const target = e.target;
            if (target?.classList.contains('receiverSelectedCheck')) {
                const parentContainer = target?.closest('.infoReceiverContainer');
                const additionalOptionContainer = parentContainer?.querySelector('.passedOnAdditionalOptionsContainer');
                if (additionalOptionContainer) {
                    additionalOptionContainer.hidden = !target?.checked;
                }
            }
        })
    }

    this.showInfoRecieversElementFor = function (container, show) {
        const infoReceiversDiv = container?.querySelector(".infoReceiversDiv");
        const infoReceiversCommentDiv = container?.querySelector(".infoReceiverCommentDiv");
        infoReceiversDiv.hidden = !show;
        infoReceiversCommentDiv.hidden = !show;
    }

}
