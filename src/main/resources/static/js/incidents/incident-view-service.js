import OnUnSubmittedService from "../on-unsubmitted-changes-service.js";

let onUnSubmittedService = new OnUnSubmittedService();
const incidentViewService = new IncidentViewService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentViewService.init();
});

// Requires incident-service also
function IncidentViewService() {

    this.init = () => {
        const form = document.getElementById(formId);
        form.addEventListener("submit", (event) => incidentService.validateFormBeforeSubmit(event, form));

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

}