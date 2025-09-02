import FormValidationService from "../FormValidationService.js";

window.incidentService = new IncidentService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentService.init();
});

function IncidentService() {
    this.init = () => {
        if (document.getElementById('createIncidentDialog')) {
            this.fetchDialog(formUrl, "createIncidentDialog");
        }
    }

    this.fetchDialog = (url, targetId) => {
        let self = this;
        return fetchHtml(url, targetId)
            .then(() => {
                self.initChoicesAndDatePickers(targetId);

                const modal = document.getElementById(targetId);
                const form = modal.querySelector("form");
                form.addEventListener("submit", (event) => self.validateFormBeforeSubmit(event, form));
            })
    }

    this.initChoicesAndDatePickers = (targetId) => {
        this.initDatePickers(targetId);
        this.initAssetChoices(targetId);
        this.initUserChoices(targetId);
        this.initOrganizationChoices(targetId);
        this.initSupplierChoices(targetId);
        this.initCustomChoiceLists(targetId);
    }

    this.editIncident = (targetId, incidentId) => {
        const container = document.getElementById(targetId)
        if (container) {
            container.innerText = '';
            this.fetchDialog(`${formUrl}?id=${incidentId}`, targetId)
                .then(() => {
                    let dialog = document.getElementById(targetId);
                    let editDialog = new bootstrap.Modal(dialog);
                    editDialog.show();
                })
        }
    }

    this.deleteIncident = (grid, targetId, name) => {
        Swal.fire({
            text: `Er du sikker på du vil slette hændelsen '${name}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                deleteData(`${restUrl}${targetId}`)
                    .then(response => grid.forceRender())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.fetchColumnName =  () => {
        return jsonCall('GET', restUrl + 'columns', null)
            .then((response) => {
                defaultResponseErrorHandler(response);
                return response.json();
            })
            .catch(defaultErrorHandler)
    }

    this.initDatePickers = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let pickers = dialog.querySelectorAll('.dateTimePicker');
        pickers.forEach(picker => {
            let id = picker.getAttribute('id');
            let buttonId = picker.parentElement.querySelector('button').getAttribute('id');
            initDatepicker(`#${buttonId}`, `#${id}` );
        })
    }

    this.initAssetChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let assetSelects = dialog.querySelectorAll('.assetSelect');
        assetSelects.forEach(select => {
            select.choices = choiceService.initAssetSelect(select.getAttribute('id'), false);
        })
    }

    this.initCustomChoiceLists = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let customSelect = dialog.querySelectorAll('.choiceList');
        customSelect.forEach(select => {
            let choices = new Choices(select, {
                searchChoices: false,
                removeItemButton: true,
                allowHTML: true,
                shouldSort: false,
                searchPlaceholderValue: 'Søg...',
                itemSelectText: 'Vælg',
                duplicateItemsAllowed: false,
                classNames: {
                    containerInner: 'form-control'
                }
            });
            select.addEventListener("change",
                function(event) {
                    choices.hideDropdown();
                },
                false,
            );
            select.choices = choices;
        })
    }

    this.initSupplierChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let assetSelects = dialog.querySelectorAll('.supplierSelect');
        assetSelects.forEach(select => {
            select.choices = choiceService.initSupplierSelect(select.getAttribute('id'), false);
        })
    }

    this.initUserChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let userSelects = dialog.querySelectorAll('.userSelect');
        userSelects.forEach(select => {
            select.choices = choiceService.initUserSelect(select.getAttribute('id'), false);
        })
    }

    this.initOrganizationChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let ouSelects = dialog.querySelectorAll('.organizationSelect');
        ouSelects.forEach(select => {
            select.choices = choiceService.initOUSelect(select.getAttribute('id'), false);
        })
    }

    this.setFieldValidity = (field, feedback, isValid) => {
        if (isValid) {
            field.classList.remove("is-invalid");
            if (feedback) feedback.style.display = "none";
        } else {
            field.classList.add("is-invalid");
            if (feedback) feedback.style.display = "block";
        }
    };

    this.validateFormBeforeSubmit = (event, form) => {
        let valid = true;
        let invalidFields = [];

        // validate name field
        const nameInput = form.querySelector('input[name="name"]');
        if (nameInput) {
            const val = nameInput.value.trim();
            const feedback = nameInput.parentElement.querySelector('.invalid-feedback');
            const isValid = val !== "" && val.length <= 768;

            if (!isValid) {
                valid = false;
                invalidFields.push(nameInput);
            }
            incidentService.setFieldValidity(nameInput, feedback, isValid);
        }

        // validate textField textarea max length
        const maxLength = 65000;
        const textAreas = form.querySelectorAll("textarea.textField");
        textAreas.forEach(textArea => {
            const val = textArea.value.trim();
            const feedback = textArea.parentElement.querySelector('.invalid-feedback');
            const isValid = val.length <= maxLength;

            if (!isValid) {
                valid = false;
                invalidFields.push(textArea);
            }
            incidentService.setFieldValidity(textArea, feedback, isValid);
        });

        // validate date fields
        const dateFields = form.querySelectorAll(".dateTimePicker");
        dateFields.forEach(input => {
            const val = input.value.trim();
            const feedback = input.parentElement.querySelector('.invalid-feedback');
            const isValid = val === "" || isValidDateDMY(val);

            if (!isValid) {
                valid = false;
                invalidFields.push(input);
            }
            incidentService.setFieldValidity(input, feedback, isValid);
        });

        // Validate obligatory fields
        const fvs = new FormValidationService(form)
        fvs.removeValidationMessages()
        if (!fvs.validate_isNotEmpty()) {
            valid = false;
        }

        if (!valid) {
            event.preventDefault();
            if (invalidFields.length > 0) {
                invalidFields[0].scrollIntoView({behavior: 'smooth', block: 'center'});
                invalidFields[0].focus();
            }
        }
    };
}
