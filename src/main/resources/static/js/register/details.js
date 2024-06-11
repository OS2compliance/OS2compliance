
let userChoices;
let ouChoices;
let departmentChoices;

function setPurposeEditState(editable) {
    const editBtn = document.querySelector('#editPurposeBtn');
    const cancelBtn = document.querySelector('#cancelPurposeBtn');
    const saveBtn = document.querySelector('#savePurposeBtn');
    const purpose = document.querySelector('#purpose');
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
    if (!editable) {
        const form = document.querySelector('#editPurposeId');
        form.reset();
    }
}

function setGenereltEditState(editable) {
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

function setDataprocessingEditState(editable) {
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

function setAssessmentEditState(editable) {
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

function clearColors(element) {
    element.classList.remove('btn-danger');
    element.classList.remove('btn-success');
    element.classList.remove('btn-yellow');
    element.classList.remove('btn-warning');
    element.classList.remove('bg-red');
    element.classList.remove('bg-yellow');
    element.classList.remove('bg-green');
}

function clearTitle(btnElement) {
    btnElement.removeAttribute('title');
}

function updateTitleFor(btnElement, value) {
    let consequenceScaleElement = consequenceScale[value-1];
    if (consequenceScaleElement !== undefined) {
        btnElement.setAttribute('title', consequenceScaleElement);
    }
}

function updateColorFor(btnElement, value) {
    clearColors(btnElement);
    if (value === 'GRØN') {
        btnElement.classList.add('btn-success');
    } else if (value === 'GUL') {
        btnElement.classList.add('btn-yellow');
    } else if (value === 'RØD') {
        btnElement.classList.add('btn-danger');
    } else {
        btnElement.classList.add('btn-danger');
    }
}

function updateConsequenceStatus(value) {
    const assessmentBadge = document.getElementById('assessmentBadge');
    const assessmentElement = document.getElementById('assessment');
    clearColors(assessmentBadge);
    const ival = asIntOrDefault(value, 0);
    if (ival === 1) {
        assessmentBadge.classList.add('bg-green');
        assessmentElement.value = 'GREEN';
    } else if (ival > 1 && ival < 4) {
        assessmentBadge.classList.add('bg-yellow');
        assessmentElement.value = 'YELLOW';
    } else if (ival) {
        assessmentBadge.classList.add('bg-red');
        assessmentElement.value = 'RED';
    }
}

function updateOrganisationAssessmentAvg() {
    function orgAvg(elem1, elem2) {
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
        let avg = orgAvg(rep, eco);
        if (avg !== undefined) {
            targetBtn.innerText = "" + avg;
            targetInput.value = avg;
            let value = asIntOrDefault(avg, 0);
            updateColorFor(targetBtn, scaleMap[value]);
            updateTitleFor(targetBtn, value);
        } else {
            targetBtn.innerText = "-";
            targetInput.value = '';
            clearTitle(targetBtn);
            clearColors(targetBtn);
        }
    }
    updateForField('confidentialityOrganisation');
    updateForField('integrityOrganisation');
    updateForField('availabilityOrganisation');
}

function updateAssessmentTotalAvg() {
    const averageOrganisation = document.getElementById('averageOrganisation');
    const averageRegistered = document.getElementById('averageRegistered');
    const confidentialityRegistered = document.getElementById('confidentialityRegistered');
    const integrityRegistered = document.getElementById('integrityRegistered');
    const availabilityRegistered = document.getElementById('availabilityRegistered');
    const confidentialityOrganisation = document.getElementById('confidentialityOrganisation');
    const integrityOrganisation = document.getElementById('integrityOrganisation');
    const availabilityOrganisation = document.getElementById('availabilityOrganisation');

    let divisor = 0, totalDivisor = 0;
    let sum = 0, totalSum = 0;
    function addToAvg(element) {
        let value = parseInt(element.value);
        if (!isNaN(value)) {
            divisor += 1; totalDivisor += 1;
            sum += value; totalSum += value;
        }
    }
    function updateTargets(element) {
        if (divisor !== 0) {
            let avgRegistered = Math.round(100 * (sum / divisor)) / 100;
            element.innerText = "" + avgRegistered;
            let value = asIntOrDefault(avgRegistered,0);
            updateColorFor(element, scaleMap[value]);
            updateTitleFor(element, value);
        } else {
            element.innerText = "-";
            clearColors(element);
            clearTitle(element);
        }
    }
    addToAvg(confidentialityRegistered);
    addToAvg(integrityRegistered);
    addToAvg(availabilityRegistered);
    updateTargets(averageRegistered);
    divisor = sum = 0;
    addToAvg(confidentialityOrganisation);
    addToAvg(integrityOrganisation);
    addToAvg(availabilityOrganisation);
    updateTargets(averageOrganisation);

    if (totalDivisor > 0) {
        let avg = Math.round(totalSum / totalDivisor);
        updateConsequenceStatus(avg);
    }
}

function updateAssessmentTitles() {
    function updateFor(elemId) {
        const elemBtn = document.getElementById(elemId + 'Btn');
        const elem = document.getElementById(elemId);
        updateTitleFor(elemBtn, elem.value);
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

function updateAssessmentColors() {
    function updateFor(elemId) {
        const elemBtn = document.getElementById(elemId + 'Btn');
        const elem = document.getElementById(elemId);
        updateColorFor(elemBtn, scaleMap[elem.value]);
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

function assessmentFormLoaded() {
    updateAssessmentTitles();
    updateAssessmentColors();
    updateOrganisationAssessmentAvg();
    updateAssessmentTotalAvg();
}

function updatedAssessmentValue(value, property, color) {
    const propertyElement = document.getElementById(property);
    const propertyBtnElement = document.getElementById(property + 'Btn');
    propertyElement.value = value;
    propertyBtnElement.innerText = value;
    updateTitleFor(propertyBtnElement, value);
    updateColorFor(propertyBtnElement, color);
    updateOrganisationAssessmentAvg();
    updateAssessmentTotalAvg();
}

function purposeFormLoaded() {

}

function generalFormLoaded() {
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
}
