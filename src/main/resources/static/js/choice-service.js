import { debounce } from "./debounce-service.js";

class ChoiceService {
    static #SEARCH_DEBOUNCE_MS = 150;

    /** Formats a user as shown in all user dropdowns: "(userId) name". */
    userLabel(user) {
        return `(${user.userId}) ${user.name}`;
    }

    /** Selects a value, adding it as an option first if it is not among the loaded options. */
    setSelected(choices, value, label) {
        // Select the option if it is already loaded
        choices.setChoiceByValue(value);
        if (this.#selectedValues(choices).includes(value)) {
            return;
        }
        // Otherwise add it as a new option and select it
        choices.setChoices([{ value, label, selected: true }], 'value', 'label', false);
    }

    /**
     * Fills the department select with the user's suggested department
     */
    bindOuToUser(userChoices, ouChoices) {
        const isOnlySelectedUser = (userUuid) => {
            const selected = this.#selectedValues(userChoices);
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

    updateDocumentRelations(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/relatable/autocomplete', { types: 'DOCUMENT', search }),
            (reg) => ({ value: reg.id, label: truncateString(reg.name, 60) }));
    }

    updateUsers(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/users/autocomplete', { search }),
            (user) => ({ value: user.uuid, label: this.userLabel(user) }));
    }

    updateOus(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/ous/autocomplete', { search }),
            (ou) => ({ value: ou.uuid, label: ou.name }));
    }

    updateRelations(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/relatable/autocomplete', { search }),
            (reg) => ({ value: String(reg.id), label: truncateString(`${reg.typeMessage}: ${reg.name}`, 60) }));
    }

    updateAssets(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/relatable/autocomplete', { types: 'ASSET', search, dir: 'ASC', sort: 'name' }),
            (asset) => ({ value: asset.id, label: `${asset.name}` }));
    }

    updateSuppliers(choices, search) {
        return this.#loadChoices(choices, this.#url('/rest/suppliers/autocomplete', { search }),
            (supplier) => ({ value: supplier.id, label: `${supplier.name}` }));
    }

    updateRelationsFor(choices, url) {
        return this.#loadChoices(choices, url, (reg) => this.#toRelationChoice(reg));
    }

    updateRelationsForDocument(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'TASK,SUPPLIER,ASSET,REGISTER,STANDARD_SECTION', search);
    }

    updateRelationsAssetsOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'ASSET', search);
    }

    updateRelationsTasksOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'TASK', search);
    }

    updateRelationsIncidentsOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'INCIDENT', search);
    }

    updateRelationsDocumentsOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'DOCUMENT', search);
    }

    updateRelationsRegistersOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'REGISTER', search);
    }

    updateRelationsForStandardSection(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'TASK,DOCUMENT,STANDARD_SECTION', search);
    }

    updateRelationsPrecautionsOnly(choices, search) {
        return this.#updateRelationsOfTypes(choices, 'PRECAUTION', search);
    }

    initUserSelect(elementId, prefetch = true) {
        return this.#initRemoteSelect(elementId, (choices, search) => this.updateUsers(choices, search), prefetch);
    }

    initOUSelect(elementId, prefetch = true) {
        return this.#initRemoteSelect(elementId, (choices, search) => this.updateOus(choices, search), prefetch);
    }

    initAssetSelect(elementId, prefetch = true) {
        return this.#initRemoteSelect(elementId, (choices, search) => this.updateAssets(choices, search), prefetch);
    }

    initSupplierSelect(elementId, prefetch = true) {
        return this.#initRemoteSelect(elementId, (choices, search) => this.updateSuppliers(choices, search), prefetch);
    }

    /** Initializes the document relation dropdown and reloads its options after each selection change. */
    initDocumentRelationSelect() {
        const relationsChoice = this.#initRemoteSelect('relationsSelect',
            (choices, search) => this.updateRelationsForDocument(choices, search));
        relationsChoice.passedElement.element.addEventListener('change',
            () => this.updateRelationsForDocument(relationsChoice, ''));
    }

    /** Returns the selected values as an array for both single and multiple selects, without empty placeholders. */
    #selectedValues(choices) {
        return [choices.getValue(true)].flat().filter(Boolean);
    }

    /** Maps a relation to an option labelled "type: name", shortened in the middle. */
    #toRelationChoice(reg) {
        return {
            value: reg.id,
            label: truncateMiddleString(`${reg.typeMessage}: ${reg.name}`, 80, 30, 50)
        };
    }

    /** Builds a URL with URL-encoded query parameters. */
    #url(path, params) {
        return `${path}?${new URLSearchParams(params)}`;
    }

    #updateRelationsOfTypes(choices, types, search) {
        return this.updateRelationsFor(choices, this.#url('/rest/relatable/autocomplete', { types, search }));
    }

    /**
     * Replaces the unselected options with the results from the URL; selected options are kept.
     */
    async #loadChoices(choices, url, toChoice) {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Kunne ikke hente valgmuligheder (${response.status})`);
            }
            const data = await response.json();
            choices.setChoices(data.content.map(toChoice), 'value', 'label', true);
        } catch (error) {
            toastService.error(error);
        }
    }

    /**
     * Initializes a dropdown whose options are searched on the server. Optionally loads the first
     * options right away
     */
    #initRemoteSelect(elementId, update, prefetch = true) {
        const element = document.getElementById(elementId);
        const choices = initSelect(element);
        if (prefetch) {
            update(choices, '');
        }
        element.addEventListener('search',
            debounce((event) => update(choices, event.detail.value), ChoiceService.#SEARCH_DEBOUNCE_MS));
        return choices;
    }
}

export const choiceService = new ChoiceService();
window.choiceService = choiceService;
