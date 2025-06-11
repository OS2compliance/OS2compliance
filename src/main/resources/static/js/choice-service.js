
let choiceService = new ChoiceService();
function ChoiceService() {

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

    this.updateUsers = (targetChoice, search) => {
        fetch( `/rest/users/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    targetChoice.setChoices(data.content.map(e => {
                        return {
                            value: e.uuid,
                            label: `(${e.userId}) ${e.name}`}
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

    this.updateTags = (choices, search) => {
        fetch( `/rest/relatable/tags/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    choices.setChoices(data.content.map(reg => {
                        return {
                            id: reg.id + "",
                            name: reg.value
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

    this.updateSuppliers = (choices, search) => {
        fetch( `/rest/suppliers/autocomplete?search=${search}`)
            .then(response => response.json()
                .then(data => {
                    choices.setChoices(data.content.map(e => {
                        return {
                            value: e.id,
                            label: `${e.name}`}
                    }), 'value', 'label', true);
                }))
            .catch(error => toastService.error(error));
    }

    this.initUserSelect = (elementId, prefetch = true) => {
        let self = this;
        const userSelect = document.getElementById(elementId);
        const userChoices = initSelect(userSelect);
        if (prefetch) {
            this.updateUsers(userChoices, "");
        }
        userSelect.addEventListener("search",
            function(event) {
                self.updateUsers(userChoices, event.detail.value);
            },
            false,
        );
        return userChoices;
    }

    this.initOUSelect = (elementId, prefetch = true) => {
        const ouSelect = document.getElementById(elementId);
        const ouChoices = initSelect(ouSelect);
        if (prefetch) {
            choiceService.updateOus(ouChoices, "");
        }
        ouSelect.addEventListener("search",
            function(event) {
                choiceService.updateOus(ouChoices, event.detail.value);
            },
            false,
        );
        return ouChoices;
    }

    this.initTagSelect = (id) => {
        console.log('initTagSelect ' + id)
        const tagsSelect = document.getElementById(id);
        console.log(tagsSelect)
        const tagsChoice = initSelect(tagsSelect);
        this.updateTags(tagsChoice, "");
        tagsSelect.addEventListener("search",
            function(event) {
                choiceService.updateTags(tagsChoice, event.detail.value);
            },
            false,
        );
        tagsSelect.addEventListener("change",
            function(event) {
                choiceService.updateTags(tagsChoice, "");
            },
            false,
        );
    }

    this.initDocumentRelationSelect = () => {
        const relationsSelect = document.getElementById('relationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelationsForDocument(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                choiceService.updateRelationsForDocument(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function() {
                choiceService.updateRelationsForDocument(relationsChoice, "");
            },
            false,
        );
    }

    this.initAssetSelect = (elementId, prefetch = true) => {
        let self = this;
        const assetSelect = document.getElementById(elementId);
        const assetChoices = initSelect(assetSelect);
        if (prefetch) {
            this.updateAssets(assetChoices, "");
        }
        assetSelect.addEventListener("search",
            function(event) {
                self.updateAssets(assetChoices, event.detail.value);
            },
            false,
        );
        return assetChoices;
    }

    this.initSupplierSelect = (elementId, prefetch = true) => {
        let self = this;
        const supplierSelect = document.getElementById(elementId);
        const supplierChoices = initSelect(supplierSelect);
        if (prefetch) {
            this.updateSuppliers(supplierChoices, "");
        }
        supplierSelect.addEventListener("search",
            function(event) {
                self.updateSuppliers(supplierChoices, event.detail.value);
            },
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
                            name: truncateString(reg.typeMessage + ": " + reg.name, 60)
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
        this.updateAssets(choices, `/rest/relatable/autocomplete?types=DOCUMENT&search=${search}`);
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
