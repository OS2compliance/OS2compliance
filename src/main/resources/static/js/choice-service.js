import { debounce } from "./debounce-service.js";

const SEARCH_DEBOUNCE_MS = 150;

export const choiceService = new ChoiceService();
window.choiceService = choiceService;

function ChoiceService() {
    // Select values function
    const selectedValues = (choices) => [].concat(choices.getValue(true)).filter(Boolean);

    // Label format extractor
    this.userLabel = (user) => `(${user.userId}) ${user.name}`;

    this.setSelected = (choices, value, label) => {
        // Select the option if it is already loaded
        choices.setChoiceByValue(value);
        if (selectedValues(choices).includes(value)) {
            return;
        }
        // Otherwise add it as a new option and select it
        choices.setChoices([{ value, label, selected: true }], 'value', 'label', false);
    }

    // Suggests the department of the responsible user
    this.bindOuToUser = (userChoices, ouChoices) => {
        const isOnlySelectedUser = (userUuid) => {
            const selected = selectedValues(userChoices);
            return selected.length === 1 && selected[0] === userUuid;
        };

        // triggers when a selection is added
        userChoices.passedElement.element.addEventListener('addItem', async (event) => {
            const userUuid = event.detail.value;
            // Additional users never change the department
            if (!isOnlySelectedUser(userUuid)) {
                return;
            }
            try {
                const response = await fetch(`/rest/ous/user/${encodeURIComponent(userUuid)}/suggestion`);
                // 204 means the user has no department, so the current department is left as is
                if (response.status === 204 || !response.ok) {
                    return;
                }
                const ou = await response.json();
                // The user selection may have changed while the request was in flight
                if (isOnlySelectedUser(userUuid)) {
                    this.setSelected(ouChoices, ou.uuid, ou.name);
                }
            } catch (error) {
                toastService.error(error);
            }
        });
    }

    this.updateDocumentRelations = (choices, search) => {
        fetch( `/rest/relatable/autocomplete?types=DOCUMENT&search=${search}`)
            .then(response => response.json()
                .then(data => {
                    choices.setChoices(data.content.map(reg => {
                        return {
                            id: reg.id,
                            name: truncateString(reg.name, 60)
                        }
                    }), 'id', 'name', true);
                }))
            .catch(error => toastService.error(error));
    }

    // The multiple flag is default set to false, so all single user selects have a placeholder. Multiple selects should have this flag set to true
    // because they are not supposed to have a placeholder option or else users can choose it as an option, which makes no sense.
    this.updateUsers = (targetChoice, search) => {
        fetch( `/rest/users/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    targetChoice.setChoices(data.content.map(e => {
                        return {
                            value: e.uuid,
                            label: this.userLabel(e)}
                        }), 'value', 'label', true);
                }))
            .catch(error => toastService.error(error));
    }

    this.updateOus = (targetChoice, search) => {
        fetch( `/rest/ous/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    targetChoice.setChoices(data.content, 'uuid', 'name', true);
                }))
            .catch(error => toastService.error(error));
    }

    this.updateRelations = (choices, search) => {
        fetch( `/rest/relatable/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    choices.setChoices(data.content.map(reg => {
                        return {
                            id: reg.id + "",
                            name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                        }
                    }), 'id', 'name', true);
                }))
            .catch(error => toastService.error(error));
    }



    this.updateAssets = (targetChoice, search) => {
        fetch( `/rest/relatable/autocomplete?types=ASSET&search=${search}&dir=ASC&sort=name`)
            .then(response => response.json()
                .then(data => {
                    targetChoice.setChoices(data.content.map(a => {
                        return {
                            value: a.id,
                            label: `${a.name}`}
                    }), 'value', 'label', true);
                }))
            .catch(error => toastService.error(error));
    }

    this.updateSuppliers = async (choices, search) => {
        try {
            const response = await fetch(`/rest/suppliers/autocomplete?search=${search}`);
            const data = await response.json();

            choices.setChoices(data.content.map(e => {
                return {
                    value: e.id,
                    label: `${e.name}`}
            }), 'value', 'label', true);
        } catch (error) {
            toastService.error(error);
        }
    }

    this.initUserSelect = (elementId, prefetch = true) => {
        const userSelect = document.getElementById(elementId);
        const userChoices = initSelect(userSelect);
        if (prefetch) {
            this.updateUsers(userChoices, "");
        }
        userSelect.addEventListener("search",
            debounce(event => this.updateUsers(userChoices, event.detail.value), SEARCH_DEBOUNCE_MS),
            false,
        );
        return userChoices;
    }

    this.initOUSelect = (elementId, prefetch = true) => {
        const ouSelect = document.getElementById(elementId);
        const ouChoices = initSelect(ouSelect);
        if (prefetch) {
            this.updateOus(ouChoices, "");
        }
        ouSelect.addEventListener("search",
            debounce(event => this.updateOus(ouChoices, event.detail.value), SEARCH_DEBOUNCE_MS),
            false,
        );
        return ouChoices;
    }

    this.initHeaderSelect = (elementId) => {
        const headerSelect = document.getElementById(elementId);
        initSelect(headerSelect);
    }

    this.initDocumentRelationSelect = () => {
        const relationsSelect = document.getElementById('relationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        this.updateRelationsForDocument(relationsChoice, "");
        relationsSelect.addEventListener("search",
            debounce(event => this.updateRelationsForDocument(relationsChoice, event.detail.value), SEARCH_DEBOUNCE_MS),
            false,
        );
        relationsSelect.addEventListener("change",
            () => this.updateRelationsForDocument(relationsChoice, ""),
            false,
        );
    }

    this.initAssetSelect = (elementId, prefetch = true) => {
        const assetSelect = document.getElementById(elementId);
        const assetChoices = initSelect(assetSelect);
        if (prefetch) {
            this.updateAssets(assetChoices, "");
        }
        assetSelect.addEventListener("search",
            debounce(event => this.updateAssets(assetChoices, event.detail.value), SEARCH_DEBOUNCE_MS),
            false,
        );
        return assetChoices;
    }

    this.initSupplierSelect = (elementId, prefetch = true) => {
        const supplierSelect = document.getElementById(elementId);
        const supplierChoices = initSelect(supplierSelect);
        if (prefetch) {
            this.updateSuppliers(supplierChoices, "");
        }
        supplierSelect.addEventListener("search",
            debounce(event => this.updateSuppliers(supplierChoices, event.detail.value), SEARCH_DEBOUNCE_MS),
            false,
        );
        return supplierChoices;
    }

    this.updateRelationsFor = (choices, url) => {
        fetch( url)
            .then(response => response.json()
                .then(data => {
                    choices.setChoices(data.content.map(reg => {
                        return {
                            id: reg.id,
                            name: truncateMiddleString(reg.typeMessage + ": " + reg.name, 80, 30, 50)
                        }
                    }), 'id', 'name', true);
                }))
            .catch(error => toastService.error(error));
    }

    this.updateRelationsForDocument = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=TASK,SUPPLIER,ASSET,REGISTER,STANDARD_SECTION&search=${search}`);
    }

    this.updateRelationsAssetsOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=ASSET&search=${search}`);
    }

    this.updateRelationsTasksOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=TASK&search=${search}`);
    }

    this.updateRelationsIncidentsOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=INCIDENT&search=${search}`);
    }

    this.updateRelationsDocumentsOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=DOCUMENT&search=${search}`);
    }


    this.updateRelationsRegistersOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=REGISTER&search=${search}`);
    }

    this.updateRelationsForStandardSection = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=TASK,DOCUMENT,STANDARD_SECTION&search=${search}`);
    }

    this.updateRelationsPrecautionsOnly = (choices, search) => {
        this.updateRelationsFor(choices, `/rest/relatable/autocomplete?types=PRECAUTION&search=${search}`);
    }
}
