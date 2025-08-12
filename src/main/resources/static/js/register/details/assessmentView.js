/**
 * Functionality related to the "Konsekvens- og risikovurdering" page of the register detail view
 */
export default function RegisterAssessmentService() {
    this.init = function () {
        this.updateAssessmentTitles();
        this.updateAssessmentColors();
        this.updateOrganisationAssessmentAvg();
        this.updateAssessmentTotalMax();
    }

    this.setAssessmentEditState = function (editable) {
        const editBtn = document.querySelector('#editAssessmentBtn');
        const cancelBtn = document.querySelector('#cancelAssessmentBtn');
        const saveBtn = document.querySelector('#saveAssessmentBtn');

        // Basic assessment buttons
        const confidentialityRegisteredBtn = document.querySelector('#confidentialityRegisteredBtn');
        const integrityRegisteredBtn = document.querySelector('#integrityRegisteredBtn');
        const availabilityRegisteredBtn = document.querySelector('#availabilityRegisteredBtn');
        const confidentialityOrganisationBtn = document.querySelector('#confidentialityOrganisationBtn');
        const integrityOrganisationBtn = document.querySelector('#integrityOrganisationBtn');
        const availabilityOrganisationBtn = document.querySelector('#availabilityOrganisationBtn');
        const confidentialitySocietyBtn = document.querySelector('#confidentialitySocietyBtn');
        const integritySocietyBtn = document.querySelector('#integritySocietyBtn');
        const availabilitySocietyBtn = document.querySelector('#availabilitySocietyBtn');
        const authenticitySocietyBtn = document.querySelector('#authenticitySocietyBtn');

        // Reason text areas
        const registeredReason = document.querySelector('#registeredReason');
        const organisationReason = document.querySelector('#organisationReason');
        const societyReason = document.querySelector('#societyReason');

        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';

        // Enable/disable basic buttons
        confidentialityRegisteredBtn.disabled = !editable;
        integrityRegisteredBtn.disabled = !editable;
        availabilityRegisteredBtn.disabled = !editable;
        confidentialitySocietyBtn.disabled = !editable;
        integritySocietyBtn.disabled = !editable;
        availabilitySocietyBtn.disabled = !editable;
        if (authenticitySocietyBtn) authenticitySocietyBtn.disabled = !editable;

        // Enable/disable reason text areas
        if (registeredReason) registeredReason.readOnly = !editable;
        if (organisationReason) organisationReason.readOnly = !editable;
        if (societyReason) societyReason.readOnly = !editable;

        // Enable/disable dynamic organisation assessment buttons
        this.setOrganisationAssessmentButtonsState(editable);

        if (!editable) {
            const form = document.querySelector('#editAssessmentId');
            form.reset();
        }
    }

    this.setOrganisationAssessmentButtonsState = function(editable) {
        // Find all organisation assessment buttons dynamically
        const orgButtons = document.querySelectorAll('[id*="confidentiality"][id$="Btn"], [id*="integrity"][id$="Btn"], [id*="availability"][id$="Btn"]');
        orgButtons.forEach(button => {
            // Skip the main organisation and society buttons (they're handled separately)
            if (!button.id.includes('Organisation') && !button.id.includes('Society') && !button.id.includes('Registered')) {
                button.disabled = !editable;
            }
        });
    }

    this.updateOrganisationAssessmentValue = function (value, choiceValueId, assessmentType) {
        let color = 'RØD'; // Default red for "Ikke angivet"
        if (value !== '-') {
            color = scaleMap[value]; // Get color from scale map for actual values
        }

        // Find the correct button by looking for the one that has the choiceValueId in its dropdown onclick
        let button = null;
        const allButtons = document.querySelectorAll(`[id*="${assessmentType}"][id$="Btn"]`);

        // Use for loop so we can break when we find the right button
        for (let i = 0; i < allButtons.length; i++) {
            const btn = allButtons[i];

            // Skip registered, organisation total, and society buttons
            if (btn.id.includes('Registered') || btn.id.includes('Organisation') || btn.id.includes('Society')) {
                continue;
            }

            const dropdownItems = btn.parentElement.querySelectorAll('.dropdown-item');
            let foundMatch = false;

            for (let j = 0; j < dropdownItems.length; j++) {
                const item = dropdownItems[j];
                const itemOnclick = item.getAttribute('onclick') || '';
                // Check if this dropdown item's onclick contains the specific choiceValueId
                if (itemOnclick.includes(`updateOrganisationAssessmentValue`) &&
                    itemOnclick.includes(choiceValueId)) {
                    button = btn;
                    foundMatch = true;
                    break; // Break out of dropdown items loop
                }
            }

            if (foundMatch) {
                break; // Break out of buttons loop
            }
        }

        if (button) {
            if (value !== '-') {
                button.innerText = value;
                this.updateColorFor(button, color);
                this.updateTitleFor(button, value);
            } else {
                button.innerText = '-';
                this.updateColorFor(button, color);
                this.clearTitle(button);
            }

            // Update hidden form field
            this.updateOrganisationAssessmentHiddenField(choiceValueId, assessmentType, value);

            // Recalculate organisation totals and max values
            this.updateOrganisationAssessmentAvg();
            this.updateAssessmentTotalMax();
        } else {
            console.error(`Could not find button for choiceValueId: ${choiceValueId}, assessmentType: ${assessmentType}`);
        }
    }

    this.updateOrganisationAssessmentHiddenField = function(choiceValueId, assessmentType, value) {
        const hiddenField = document.querySelector(`input[name*="organisationAssessmentColumns"][name*="${assessmentType}"][value="${choiceValueId}"]`);

        if (!hiddenField) {
            const choiceValueFields = document.querySelectorAll('input[name*="choiceValue.id"]');
            choiceValueFields.forEach(field => {
                if (field.value == choiceValueId) {
                    const indexMatch = field.name.match(/organisationAssessmentColumns\[(\d+)\]/);
                    if (indexMatch) {
                        const index = indexMatch[1];
                        const targetFieldName = `organisationAssessmentColumns[${index}].${assessmentType}`;
                        const targetField = document.querySelector(`input[name="${targetFieldName}"]`);
                        if (targetField) {
                            targetField.value = value === '-' ? '' : value;
                        }
                    }
                }
            });
        }
    }


    this.updateAssessmentTitles = function () {
        let self = this;

        function updateFor(elemId) {
            const elemBtn = document.getElementById(elemId + 'Btn');
            const elem = document.getElementById(elemId);
            if (elemBtn && elem) {
                self.updateTitleFor(elemBtn, elem.value);
            }
        }

        // Update basic assessment titles
        updateFor('confidentialityRegistered');
        updateFor('confidentialitySociety');
        updateFor('confidentialityOrganisation');
        updateFor('integrityRegistered');
        updateFor('integritySociety');
        updateFor('integrityOrganisation');
        updateFor('availabilityRegistered');
        updateFor('availabilitySociety');
        updateFor('availabilityOrganisation');
        updateFor('authenticitySociety');
    }

    this.updateAssessmentColors = function () {
        let self = this;

        function updateFor(elemId) {
            const elemBtn = document.getElementById(elemId + 'Btn');
            const elem = document.getElementById(elemId);
            if (elemBtn && elem && elem.value) {
                self.updateColorFor(elemBtn, scaleMap[elem.value]);
            }
        }

        // Update basic assessment colors
        updateFor('confidentialityRegistered');
        updateFor('confidentialitySociety');
        updateFor('confidentialityOrganisation');
        updateFor('integrityRegistered');
        updateFor('integritySociety');
        updateFor('integrityOrganisation');
        updateFor('availabilityRegistered');
        updateFor('availabilitySociety');
        updateFor('availabilityOrganisation');
        updateFor('authenticitySociety');

        // Update colors for dynamic organisation assessment buttons
        const orgButtons = document.querySelectorAll('[id*="confidentiality"][id$="Btn"], [id*="integrity"][id$="Btn"], [id*="availability"][id$="Btn"]');
        orgButtons.forEach(button => {
            // Skip registered, organisation total, and society buttons
            if (!button.id.includes('Registered') && !button.id.includes('Organisation') && !button.id.includes('Society')) {
                const buttonText = button.innerText || button.textContent;
                if (buttonText && buttonText !== '-') {
                    const value = parseInt(buttonText);
                    if (!isNaN(value) && value > 0) {
                        this.updateColorFor(button, scaleMap[value]);
                        this.updateTitleFor(button, value);
                    }
                }
            }
        });
    }

    this.enumColorToBtn = function (color) {
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

    this.updateColorFor = function (btnElement, value) {
        this.clearColors(btnElement);
        btnElement.classList.add(this.enumColorToBtn(value));
    }

    this.clearColors = function (element) {
        element.classList.remove('btn-danger');
        element.classList.remove('btn-success');
        element.classList.remove('btn-yellow');
        element.classList.remove('btn-warning');
        element.classList.remove('bg-red');
        element.classList.remove('bg-yellow');
        element.classList.remove('bg-green');
    }


    this.clearTitle = function (btnElement) {
        btnElement.removeAttribute('title');
    }

    this.updateTitleFor = function (btnElement, value) {
        let consequenceScaleElement = consequenceScale[value - 1];
        if (consequenceScaleElement !== undefined && consequenceScaleElement !== '-') {
            btnElement.setAttribute('title', consequenceScaleElement);
        }
    }

    this.updateOrganisationAssessmentAvg = function () {
        let self = this;

        // Get all organisation assessment values and calculate max for each assessment type
        function calculateOrgAssessmentMax(assessmentType) {
            const buttons = document.querySelectorAll(`[id*="${assessmentType}"][id$="Btn"]`);
            let maxValue = 0;
            let hasValue = false;

            buttons.forEach(button => {
                // Skip registered, organisation total, and society buttons
                if (!button.id.includes('Registered') && !button.id.includes('Organisation') && !button.id.includes('Society')) {
                    const buttonText = button.innerText || button.textContent;
                    if (buttonText && buttonText !== '-') {
                        const value = parseInt(buttonText);
                        if (!isNaN(value)) {
                            maxValue = Math.max(maxValue, value);
                            hasValue = true;
                        }
                    }
                }
            });

            return hasValue ? maxValue : undefined;
        }

        // Update organisation totals based on max values from dynamic organisation types
        function updateOrganisationTotal(assessmentType) {
            const maxValue = calculateOrgAssessmentMax(assessmentType);
            const targetBtn = document.getElementById(`${assessmentType}OrganisationBtn`);
            const targetInput = document.getElementById(`${assessmentType}Organisation`);

            if (targetBtn && targetInput) {
                if (maxValue !== undefined) {
                    targetBtn.innerText = "" + maxValue;
                    targetInput.value = maxValue;
                    self.updateColorFor(targetBtn, scaleMap[maxValue]);
                    self.updateTitleFor(targetBtn, maxValue);
                } else {
                    targetBtn.innerText = "-";
                    targetInput.value = '';
                    self.clearTitle(targetBtn);
                    self.clearColors(targetBtn);
                }
            }
        }

        updateOrganisationTotal('confidentiality');
        updateOrganisationTotal('integrity');
        updateOrganisationTotal('availability');
    }


    this.updateAssessmentTotalMax = function () {
        let self = this;
        const maxOrganisation = document.getElementById('maxOrganisation');
        const maxRegistered = document.getElementById('maxRegistered');
        const maxSociety = document.getElementById('maxSociety');
        const confidentialityRegistered = document.getElementById('confidentialityRegistered');
        const integrityRegistered = document.getElementById('integrityRegistered');
        const availabilityRegistered = document.getElementById('availabilityRegistered');
        const confidentialityOrganisation = document.getElementById('confidentialityOrganisation');
        const integrityOrganisation = document.getElementById('integrityOrganisation');
        const availabilityOrganisation = document.getElementById('availabilityOrganisation');
        const confidentialitySociety = document.getElementById('confidentialitySociety');
        const integritySociety = document.getElementById('integritySociety');
        const availabilitySociety = document.getElementById('availabilitySociety');
        const authenticitySociety = document.getElementById('authenticitySociety');

        function maxValue(element1, element2, element3, element4 = null) {
            let value1 = asIntOrDefault(element1.value, 0);
            let value2 = asIntOrDefault(element2.value, 0);
            let value3 = asIntOrDefault(element3.value, 0);
            let value4 = element4 == null ? 0 : asIntOrDefault(element4.value, 0);
            return Math.max(value1, value2, value3, value4);
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
        let maxSocValue = maxValue(confidentialitySociety, integritySociety, availabilitySociety, authenticitySociety);
                updateTargets(maxSociety, maxSocValue);

        let totalMax = Math.max(maxRegValue, maxOrgValue, maxSocValue);
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

    this.updateConsequenceStatus = function (value) {
        const assessmentBadge = document.getElementById('assessmentBadge');
        const assessmentElement = document.getElementById('assessment');
        this.clearColors(assessmentBadge);
        const ival = asIntOrDefault(value, 0);
        let color = scaleMap["" + ival];
        assessmentBadge.classList.add(this.enumColorToBtn(color));
        assessmentElement.value = this.enumColorToAssessment(color);
    }


    this.updatedAssessmentValue = function (value, property, color) {
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