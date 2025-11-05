import OnUnSubmittedService from "../on-unsubmitted-changes-service.js";
import { incidentService } from "./incident-service.js";

let incidentViewService;

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", function(event) {
    incidentService.init();

    const targetId = '_dm-tabsIncident';
    incidentViewService = new IncidentViewService();
    incidentViewService.init();
    incidentViewService.setEditable(targetId, false);
});

// Requires incident-service also
function IncidentViewService() {
    let onUnSubmittedService = new OnUnSubmittedService();

    this.init = () => {
        const targetId = '_dm-tabsIncident';
        incidentService.initChoicesAndDatePickers(targetId);
        incidentViewService.setEditable(targetId, false);

        const form = document.getElementById(formId);
        form.addEventListener("submit", (event) => this.validateFormBeforeSubmit(event, form));

        const editDescBtn = document.getElementById("editDescBtn");
        const cancelBtn = document.getElementById("cancelBtn");
        const saveBtn = document.getElementById("saveBtn");

        editDescBtn?.addEventListener("click", () => {
            this.setEditable('_dm-tabsIncident', true);
            onUnSubmittedService.setChangesMade();
        });

        cancelBtn?.addEventListener("click", () => {
            this.setEditable('_dm-tabsIncident', false);
            onUnSubmittedService.reset();
        });

        saveBtn?.addEventListener("click", () => {
            onUnSubmittedService.reset();
        });
    }

    this.setEditable = (dialogId, editable) => {
        const editDescBtn = document.querySelector('#editDescBtn');
        const saveBtn = document.querySelector('#saveBtn');
        const cancelBtn = document.querySelector('#cancelBtn');
        editDescBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = editable ? 'display: block' : 'display: none';
        cancelBtn.style = editable ? 'display: block' : 'display: none';

        let dialog = document.getElementById(dialogId);
        let formElements = dialog.querySelectorAll('select, input, textarea, .dateBtn');
        formElements.forEach(select => {
            select.disabled = !editable;
            let choice = select.choices;
            if (choice !== null && choice !== undefined) {
                editable ? choice.enable() : choice.disable();
            }
        });
        this.makeLinks(dialogId, !editable);
    }

    this.makeLink = (divElem, targetType, linkActive) => {
        let dataValue = divElem.dataset.value;
        if (linkActive) {
            let clickHandler = () => {
                if ("SUPPLIER" === targetType) {
                    location.href = `/suppliers/${dataValue}`;
                }
                if ("ASSET" === targetType) {
                    location.href = `/assets/${dataValue}`;
                }
            };
            divElem.style.cursor = 'pointer';
            divElem.handler = clickHandler;
            divElem.addEventListener('click', clickHandler);
        } else {
            divElem.removeEventListener('click', divElem.handler);
            divElem.style.cursor = 'not-allowed';
        }
    }

    this.makeLinks = (dialogId, linksActive) => {
        let dialog = document.getElementById(dialogId);
        let possibleLinks = dialog.querySelectorAll(".choices__item");
        possibleLinks.forEach(optionDiv => {
            let dataValue = optionDiv.dataset.value;
            let formControl = optionDiv.closest(".form-control");
            if (formControl !== null) {
                let select = formControl.querySelector("select");
                if (dataValue !== null && select !== null) {
                    for (let option of select.options) {
                        let targetType = option.dataset.type;
                        if (targetType !== undefined && option.value === dataValue) {
                            this.makeLink(optionDiv, targetType, linksActive);
                        }
                    }
                }
            }
        })
    }

    this.validateFormBeforeSubmit = (event, form) => {
        let valid = true;
        let invalidFields = [];

        // validate date field
        const dateInput = form.querySelector('input[name="riskAssessmentConductedDate"]');
        if (dateInput) {
            const val = dateInput.value.trim();
            const feedback = dateInput.parentElement.querySelector('.invalid-feedback');
            const isValid = val === "" || isValidDateDMY(val);

            if (!isValid) {
                valid = false;
                invalidFields.push(dateInput);
            }
            assetRiskKitosService.setFieldValidity(dateInput, feedback, isValid);
        }

        if (!valid) {
            event.preventDefault();
            if (invalidFields.length > 0) {
                invalidFields[0].scrollIntoView({behavior: 'smooth', block: 'center'});
                invalidFields[0].focus();
            }
        } else {
            // enable fields if disabled, to make sure they are included in the form on submit
            document.getElementById("riskAssessmentConductedDate").disabled = false;
            document.getElementById("result").disabled = false;
        }
    };

}