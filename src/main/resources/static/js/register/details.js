
let userChoices;
let ouChoices;
let departmentChoices;

/**
 * Functionality related to the "Generelt" page of the register detail view
 */
let registerGeneralService = new RegisterGeneralService();
function RegisterGeneralService() {
    this.init = function() {
        this.generalFormLoaded();
        this.initAssetRelationSelect();
        this.initDocumentRelationSelectPrivate();
        this.initTaskRelationSelectPrivate();
    }

    this.generalFormLoaded = function () {
        const form = document.getElementById('editDescId');

        departmentChoices = initOUSelect('departmentSelect', false);
        ouChoices = initOUSelect('ouSelect', false);
        userChoices = initUserSelect('userSelect', false);

        form.addEventListener('reset', (ev) => {
            ouChoices.destroy();
            ouChoices.init();
            userChoices.destroy();
            userChoices.init();
            departmentChoices.destroy();
            departmentChoices.init();
        })
    };

    this.initAssetRelationSelect = function() {
        const relationsSelect = document.getElementById('AssetRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        updateRelationsAssetsOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                updateRelationsAssetsOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function(event) {
                updateRelationsAssetsOnly(relationsChoice, "");
            },
            false,
        );
    }


    this.initDocumentRelationSelectPrivate = function() {
        const relationsSelect = document.getElementById('DocumentRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        updateRelationsDocumentsOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                updateRelationsDocumentsOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function(event) {
                updateRelationsDocumentsOnly(relationsChoice, "");
            },
            false,
        );
    }

    this.initTaskRelationSelectPrivate = function() {
        const relationsSelect = document.getElementById('TaskRelationModalrelationsSelect');
        let relationsChoice = initSelect(relationsSelect);
        updateRelationsTasksOnly(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                updateRelationsTasksOnly(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function(event) {
                updateRelationsTasksOnly(relationsChoice, "");
            },
            false,
        );
    }

    this.setGenereltEditState = function(editable) {
        const editBtn = document.querySelector('#editBtn');
        const cancelBtn = document.querySelector('#cancelBtn');
        const saveBtn = document.querySelector('#saveBtn');
        const editDesc = document.querySelector('#description');
        const criticality = document.querySelector('#criticality');
        const emergencyPlanLink = document.querySelector('#emergencyPlanLink');
        const informationResponsible = document.querySelector('#informationResponsible');
        const registerRegarding = document.querySelector('#registerRegarding');
        const status = document.querySelector('#status');

        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';

        editDesc.readOnly = !editable;
        emergencyPlanLink.readOnly = !editable;
        registerRegarding.readOnly = !editable;
        informationResponsible.readOnly = !editable;
        criticality.disabled = !editable;
        status.disabled = !editable;
        if (!editable) {
            const form = document.getElementById('editDescId');
            form.reset();
            userChoices.disable();
            ouChoices.disable();
            departmentChoices.disable();
        } else {
            userChoices.enable();
            ouChoices.enable();
            departmentChoices.enable();
        }
    }

    this.showEditRelationDialog = function(elem) {
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

/**
 * Functionality related to the "Formål og lovhjemmel" page of the register detail view
 */
let registerPurposeService = new RegisterPurposeService();
function RegisterPurposeService() {

    this.init = function() {};

    this.setPurposeEditState = function (editable) {
        const editBtn = document.querySelector('#editPurposeBtn');
        const cancelBtn = document.querySelector('#cancelPurposeBtn');
        const saveBtn = document.querySelector('#savePurposeBtn');
        const purpose = document.querySelector('#purpose');
        const purposeNotes = document.querySelector('#purposeNotes');
        const informationObligationDesc = document.querySelector('#informationObligationDesc');
        const informationObligation = document.querySelector('#informationObligation');
        const checkboxes = document.getElementsByClassName("form-check-input");
        const consent = document.querySelector('#consent');

        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';

        for (let i=0; i<checkboxes.length;++i) {
            checkboxes[i].disabled = !editable;
        }
        informationObligationDesc.readOnly = !editable;
        consent.readOnly = !editable;
        informationObligation.disabled = !editable;
        purpose.readOnly = !editable;
        purposeNotes.readOnly = !editable;
        if (!editable) {
            const form = document.querySelector('#editPurposeId');
            form.reset();
        }
    }
}

/**
 * Functionality related to the "Konsekvens- og risikovurdering" page of the register detail view
 */
let registerAssessmentService = new RegisterAssessmentService();
function RegisterAssessmentService() {
    this.init = function() {
        this.updateAssessmentTitles();
        this.updateAssessmentColors();
        this.updateOrganisationAssessmentAvg();
        this.updateAssessmentTotalMax();
    }

    this.setAssessmentEditState = function(editable) {
        const editBtn = document.querySelector('#editAssessmentBtn');
        const cancelBtn = document.querySelector('#cancelAssessmentBtn');
        const saveBtn = document.querySelector('#saveAssessmentBtn');
        const confidentialityRegisteredBtn = document.querySelector('#confidentialityRegisteredBtn');
        const confidentialityOrganisationRepBtn = document.querySelector('#confidentialityOrganisationRepBtn');
        const confidentialityOrganisationEcoBtn = document.querySelector('#confidentialityOrganisationEcoBtn');
        const integrityRegisteredBtn = document.querySelector('#integrityRegisteredBtn');
        const integrityOrganisationRepBtn = document.querySelector('#integrityOrganisationRepBtn');
        const integrityOrganisationEcoBtn = document.querySelector('#integrityOrganisationEcoBtn');
        const availabilityRegisteredBtn = document.querySelector('#availabilityRegisteredBtn');
        const availabilityOrganisationRepBtn = document.querySelector('#availabilityOrganisationRepBtn');
        const availabilityOrganisationEcoBtn = document.querySelector('#availabilityOrganisationEcoBtn');
        const availabilityReason = document.querySelector('#availabilityReason');
        const integrityReason = document.querySelector('#integrityReason');
        const confidentialityReason = document.querySelector('#confidentialityReason');
        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';
        confidentialityRegisteredBtn.disabled = !editable;
        confidentialityOrganisationRepBtn.disabled = !editable;
        confidentialityOrganisationEcoBtn.disabled = !editable;
        integrityRegisteredBtn.disabled = !editable;
        integrityOrganisationRepBtn.disabled = !editable;
        integrityOrganisationEcoBtn.disabled = !editable;
        availabilityRegisteredBtn.disabled = !editable;
        availabilityOrganisationRepBtn.disabled = !editable;
        availabilityOrganisationEcoBtn.disabled = !editable;
        availabilityReason.readOnly = !editable;
        integrityReason.readOnly = !editable;
        confidentialityReason.readOnly = !editable;

        if (!editable) {
            const form = document.querySelector('#editAssessmentId');
            form.reset();
        }
    }



    this.updateAssessmentTitles = function () {
        let self = this;
        function updateFor(elemId) {
            const elemBtn = document.getElementById(elemId + 'Btn');
            const elem = document.getElementById(elemId);
            self.updateTitleFor(elemBtn, elem.value);
        }
        updateFor('confidentialityRegistered');
        updateFor('confidentialityOrganisation');
        updateFor('confidentialityOrganisationRep');
        updateFor('confidentialityOrganisationEco');
        updateFor('integrityRegistered');
        updateFor('integrityOrganisation');
        updateFor('integrityOrganisationRep');
        updateFor('integrityOrganisationEco');
        updateFor('availabilityRegistered');
        updateFor('availabilityOrganisation');
        updateFor('availabilityOrganisationRep');
        updateFor('availabilityOrganisationEco');
    }

    this.updateAssessmentColors = function() {
        let self = this;
        function updateFor(elemId) {
            const elemBtn = document.getElementById(elemId + 'Btn');
            const elem = document.getElementById(elemId);
            self.updateColorFor(elemBtn, scaleMap[elem.value]);
        }
        updateFor('confidentialityRegistered');
        updateFor('confidentialityOrganisation');
        updateFor('confidentialityOrganisationRep');
        updateFor('confidentialityOrganisationEco');
        updateFor('integrityRegistered');
        updateFor('integrityOrganisation');
        updateFor('integrityOrganisationRep');
        updateFor('integrityOrganisationEco');
        updateFor('availabilityRegistered');
        updateFor('availabilityOrganisation');
        updateFor('availabilityOrganisationRep');
        updateFor('availabilityOrganisationEco');
    }

    this.enumColorToBtn = function(color) {
        if (color === 'GRØN') {
            return 'btn-success';
        } else if (color === 'GUL') {
            return 'btn-yellow';
        } else if (color === 'ORANGE') {
            return 'btn-warning';
        } else if (color === 'RØD') {
            return 'btn-danger';
        } else {
            return 'btn-danger';
        }
    }

    this.updateColorFor = function(btnElement, value) {
        this.clearColors(btnElement);
        btnElement.classList.add(this.enumColorToBtn(value));
    }

    this.clearColors = function(element) {
        element.classList.remove('btn-danger');
        element.classList.remove('btn-success');
        element.classList.remove('btn-yellow');
        element.classList.remove('btn-warning');
        element.classList.remove('bg-red');
        element.classList.remove('bg-yellow');
        element.classList.remove('bg-green');
    }


    this.clearTitle = function(btnElement) {
        btnElement.removeAttribute('title');
    }

    this.updateTitleFor = function(btnElement, value) {
        let consequenceScaleElement = consequenceScale[value-1];
        if (consequenceScaleElement !== undefined && consequenceScaleElement !== '-') {
            btnElement.setAttribute('title', consequenceScaleElement);
        }
    }

    this.updateOrganisationAssessmentAvg = function () {
        let self = this;
        function orgMax(elem1, elem2) {
            const value1 = parseInt(elem1.value);
            const value2 = parseInt(elem2.value);
            if (isNaN(value1) && isNaN(value2)) {
                return undefined;
            } else if (isNaN(value1)) {
                return value2;
            } else if (isNaN(value2)) {
                return value1
            } else {
                return Math.max(value1, value2);
            }
        }
        function updateForField(id) {
            const rep = document.getElementById(`${id}Rep`);
            const eco = document.getElementById(`${id}Eco`);
            const targetBtn = document.getElementById(`${id}Btn`);
            const targetInput = document.getElementById(id);
            let avg = orgMax(rep, eco);
            if (avg !== undefined) {
                targetBtn.innerText = "" + avg;
                targetInput.value = avg;
                let value = asIntOrDefault(avg, 0);
                self.updateColorFor(targetBtn, scaleMap[value]);
                self.updateTitleFor(targetBtn, value);
            } else {
                targetBtn.innerText = "-";
                targetInput.value = '';
                self.clearTitle(targetBtn);
                self.clearColors(targetBtn);
            }
        }
        updateForField('confidentialityOrganisation');
        updateForField('integrityOrganisation');
        updateForField('availabilityOrganisation');
    }


    this.updateAssessmentTotalMax = function() {
        let self = this;
        const maxOrganisation = document.getElementById('maxOrganisation');
        const maxRegistered = document.getElementById('maxRegistered');
        const confidentialityRegistered = document.getElementById('confidentialityRegistered');
        const integrityRegistered = document.getElementById('integrityRegistered');
        const availabilityRegistered = document.getElementById('availabilityRegistered');
        const confidentialityOrganisation = document.getElementById('confidentialityOrganisation');
        const integrityOrganisation = document.getElementById('integrityOrganisation');
        const availabilityOrganisation = document.getElementById('availabilityOrganisation');

        function maxValue(element1, element2, element3) {
            let value1 = asIntOrDefault(element1.value, 0);
            let value2 = asIntOrDefault(element2.value, 0);
            let value3 = asIntOrDefault(element3.value, 0);
            return Math.max(value1, value2, value3);
        }
        function updateTargets(element, value) {
            if (value !== undefined && value > 0 && value !== '-') {
                element.innerText = "" + value;
                self.updateColorFor(element, scaleMap[value]);
                self.updateTitleFor(element, value);
            } else {
                element.innerText = "-";
                self.clearColors(element);
                self.clearTitle(element);
            }
        }

        let maxRegValue = maxValue(confidentialityRegistered, integrityRegistered, availabilityRegistered);
        updateTargets(maxRegistered, maxRegValue);
        let maxOrgValue = maxValue(confidentialityOrganisation, integrityOrganisation, availabilityOrganisation);
        updateTargets(maxOrganisation, maxOrgValue);

        let totalMax = Math.max(maxRegValue, maxOrgValue);
        if (totalMax > 0) {
            this.updateConsequenceStatus(totalMax);
        }
    }


    this.enumColorToAssessment = function (color) {
        if (color === 'GRØN') {
            return 'GREEN';
        } else if (color === 'GUL') {
            return 'YELLOW';
        } else if (color === 'ORANGE') {
            return 'ORANGE';
        } else if (color === 'RØD') {
            return 'RED';
        } else {
            return 'RED';
        }
    };

    this.updateConsequenceStatus = function(value) {
        const assessmentBadge = document.getElementById('assessmentBadge');
        const assessmentElement = document.getElementById('assessment');
        this.clearColors(assessmentBadge);
        const ival = asIntOrDefault(value, 0);
        let color = scaleMap["" + ival];
        assessmentBadge.classList.add(this.enumColorToBtn(color));
        assessmentElement.value = this.enumColorToAssessment(color);
    }


    this.updatedAssessmentValue = function(value, property, color) {
        const propertyElement = document.getElementById(property);
        const propertyBtnElement = document.getElementById(property + 'Btn');
        if (value !== '-') {
            propertyElement.value = value;
            propertyBtnElement.innerText = value;
        } else {
            propertyElement.value = '';
            propertyBtnElement.innerText = '-';
        }

        this.updateTitleFor(propertyBtnElement, value);
        this.updateColorFor(propertyBtnElement, color);
        this.updateOrganisationAssessmentAvg();
        this.updateAssessmentTotalMax();
    }

}

let registerDataprocessingService = new RegisterDataprocessingService();
function RegisterDataprocessingService() {
    this.init = function() {}

    this.setDataprocessingEditState = function(editable) {
        const rootElement = document.getElementById('dataprocessingForm');
        document.getElementById('saveDataProcessingBtn').hidden = !editable;
        document.getElementById('cancelDataProcessingBtn').hidden = !editable;
        document.getElementById('editDataProcessingBtn').hidden = editable;
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = !editable;
            if (elem.tagName === "A") {
                elem.hidden = editable;
                elem.nextElementSibling.hidden = !editable;
            }
        });
        if (!editable) {
            rootElement.reset();
        }
        editModeCategoryInformationEditable(editable);
    }

}

document.addEventListener("DOMContentLoaded", function() {
    registerGeneralService.init();
    registerPurposeService.init();
    registerAssessmentService.init();
    registerDataprocessingService.init();
});
