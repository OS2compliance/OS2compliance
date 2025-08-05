import KLESelectionService from "./kleSelectionService.js";

let ouChoices;
let departmentChoices;
let userChoices;
let customResponsibleUserChoices;
let registerRegardingChoices;
let kleService

/**
 * Functionality related to the "Generelt" page of the register detail view
 */
export default function RegisterGeneralService() {
    this.init = function () {
        this.generalFormLoaded();
        this.initAssetRelationSelect();
        this.initDocumentRelationSelectPrivate();
        this.initTaskRelationSelectPrivate();
    }


    this.generalFormLoaded = function () {
        const form = document.getElementById('editDescId');

        departmentChoices = choiceService.initOUSelect('departmentSelect', false);
        ouChoices = choiceService.initOUSelect('ouSelect', false);
        userChoices = choiceService.initUserSelect('userSelect', false);
        customResponsibleUserChoices = choiceService.initUserSelect('customUserField', false);

        const registerRegardingElement = document.getElementById('registerRegarding');
        registerRegardingChoices = initSelect(registerRegardingElement);

        form.addEventListener('reset', (ev) => {
            ouChoices.destroy();
            ouChoices.init();
            userChoices.destroy();
            userChoices.init();
            customResponsibleUserChoices.destroy();
            customResponsibleUserChoices.init();
            departmentChoices.destroy();
            departmentChoices.init();
            registerRegardingChoices.destroy();
            registerRegardingChoices.init();
        })

        kleService = new KLESelectionService()
        kleService.initKLEMainGroupSelect()
    };


    this.initAssetRelationSelect = function () {
        const relationsSelect = document.getElementById('AssetRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelationsAssetsOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function (event) {
                choiceService.updateRelationsAssetsOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function (event) {
                choiceService.updateRelationsAssetsOnly(relationsChoice, "");
            },
            false,
        );
    }


    this.initDocumentRelationSelectPrivate = function () {
        const relationsSelect = document.getElementById('DocumentRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelationsDocumentsOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function (event) {
                choiceService.updateRelationsDocumentsOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function (event) {
                choiceService.updateRelationsDocumentsOnly(relationsChoice, "");
            },
            false,
        );
    }

    this.initTaskRelationSelectPrivate = function () {
        const relationsSelect = document.getElementById('TaskRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelationsTasksOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function (event) {
                choiceService.updateRelationsTasksOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function (event) {
                choiceService.updateRelationsTasksOnly(relationsChoice, "");
            },
            false,
        );
    }

    this.setGenereltEditState = function (editable) {
        const editBtn = document.querySelector('#editBtn');
        const cancelBtn = document.querySelector('#cancelBtn');
        const saveBtn = document.querySelector('#saveBtn');
        const editDesc = document.querySelector('#description');
        const criticality = document.querySelector('#criticality');
        const emergencyPlanLink = document.querySelector('#emergencyPlanLink');
        const informationResponsible = document.querySelector('#informationResponsible');
        const dataProtectionOfficer = document.querySelector('#dataProtectionOfficer');
        const registerRegarding = document.querySelector('#registerRegarding');
        const status = document.querySelector('#status');
        const securityPrecautions = document.getElementById('securityPrecautions')
        const nameField = document.getElementById('registerNameField');

        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';

        editDesc.readOnly = !editable;
        emergencyPlanLink.readOnly = !editable;
        registerRegarding.readOnly = !editable;
        informationResponsible.readOnly = !editable;
        nameField.disabled = !editable;
        registerRegarding.readOnly = !editable;
        informationResponsible.readOnly = !editable;
        dataProtectionOfficer.readOnly = !editable;
        criticality.disabled = !editable;
        status.disabled = !editable;
        securityPrecautions.readOnly = !editable;
        if (!editable) {
            const form = document.getElementById('editDescId');
            form.reset();
            userChoices.disable();
            customResponsibleUserChoices.disable();
            ouChoices.disable();
            departmentChoices.disable();
            registerRegardingChoices.disable();
            kleService.mainGroupSelectorInstance.disable();
            kleService.groupSelectorInstance.disable();
        } else {
            userChoices.enable();
            customResponsibleUserChoices.enable();
            ouChoices.enable();
            departmentChoices.enable();
            registerRegardingChoices.enable();
            kleService.mainGroupSelectorInstance.enable();
            kleService.groupSelectorInstance.enable();
        }
    }

    this.showEditRelationDialog = function (elem) {
        const registerId = elem.getAttribute('data-relatableid');
        const relationId = elem.getAttribute('data-relationid');
        const relationType = elem.getAttribute('data-relationtype');
        fetch(`/registers/${registerId}/relations/${relationId}/${relationType}`)
            .then(response => response.text()
                .then(data => {
                    let holder = document.getElementById('EditRelationModalHolder');
                    holder.innerHTML = data;
                    let editRelationModalDialog = document.getElementById('EditRelationModal');
                    let relationsSelect = editRelationModalDialog.querySelector('#EditRelationModalrelationsSelect');
                    let relationsChoice = initSelect(relationsSelect);

                    const editRelationModal = new bootstrap.Modal(editRelationModalDialog);
                    editRelationModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.editRiskScaleUpdated = function (value) {
        let percentElement = document.getElementById('EditRelationModalRiskScalePercent');
        percentElement.value = value + "%";
    }

}