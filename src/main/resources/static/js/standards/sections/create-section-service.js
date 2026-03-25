const createSectionService = new CreateSectionService();

document.addEventListener("shown.bs.modal", function(event) {
    const form = document.getElementById("headerForm");
    if (form) {
        form.addEventListener("submit", function(event) {
            event.preventDefault();

            form.classList.add("was-validated");

            const inputField = document.getElementById("sectionNumberInput");

            // Validate section format, length and characters
            const maxLength = 10;
            const maxInteger = 2147483647;
            const sectionValue = form.elements['section'].value;

            let validated = ValidateSection(sectionValue, maxLength, maxInteger);

            // Check if form is valid or not and act accordingly
            if (!validated.isValid) {
                inputField.classList.add("is-invalid");
                inputField.classList.remove("is-valid");

                const feedback = inputField.parentNode.querySelector(".invalid-feedback");
                if (feedback) {
                    feedback.textContent = validated.errorMessage;
                }

                return;
            } else {
                inputField.classList.remove("is-invalid");
                inputField.classList.add("is-valid");
            }

            if (!form.checkVisibility()) {
                return;
            }

            form.submit();
        });
    }
});

function CreateSectionService() {
    this.sectionModalDialog = null;
    this.headerModalDialog = null;

    this.openRequirementModal = function(element) {
        const id = element.dataset.id;
        fetch(`/standards/section/form/` + id)
            .then(response => response.text()
                .then(data => {
                    this.sectionModalDialog = document.getElementById('sectionFormDialog');
                    this.sectionModalDialog.innerHTML = data;
                    const createSectionModal = new bootstrap.Modal(this.sectionModalDialog);
                    createSectionModal.show();

                    // Add form validation after modal is shown
                    const form = this.sectionModalDialog.querySelector('form');
                    const submitButton = this.sectionModalDialog.querySelector('button[type="submit"]');

                    submitButton.addEventListener('click', (e) => {
                        e.preventDefault();

                        const headerSelect = form.querySelector('#headerSelect');

                        if (!headerSelect.value || headerSelect.value === '') {
                            toastService.error('Vælg venligst en gruppe');
                            return false;
                        }

                        // If validation passes, submit the form
                        form.submit();
                    });
                }))
            .catch(error => toastService.error(error));
    }

    this.openRequirementEditModal = function(element) {
        const sectionIdentifier = element.dataset.sectionid;
        const templateId = element.dataset.templateid;
        fetch(`/standards/section/form/${templateId}/${sectionIdentifier}`)
            .then(response => response.text()
                .then(data => {
                    this.sectionModalDialog = document.getElementById('sectionFormDialog');
                    this.sectionModalDialog.innerHTML = data;
                    const modal = new bootstrap.Modal(this.sectionModalDialog);
                    modal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openHeaderModal = function(element, isEdit = false) {
        const id = element.dataset.id;
        let url = "/standards/section/header/form/" + id;
        if (isEdit) {
            const headerId = element.dataset.header;
            url = "/standards/section/header/form/" + id + "/" + headerId;
        }
        fetch(url)
            .then(response => response.text()
                .then(data => {
                    this.headerModalDialog = document.getElementById('headerFormDialog');
                    this.headerModalDialog.innerHTML = data;
                    const headerModal = new bootstrap.Modal(this.headerModalDialog);
                    headerModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openDeleteSwal = function (element, isheader=true) {
        const id = element.dataset.id;
        const path = isheader === true ? "header/" : "section/";
        const deleteText = isheader === true ? "Er du sikker på du vil slette denne gruppe? Alle underliggende krav vil også slettes" : "Er du sikker på du vil slette dette krav?";
        Swal.fire({
            text: deleteText,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch("/rest/standards/" + path + "delete/" + id, { method: 'POST', headers: { 'X-CSRF-TOKEN': token} })
                    .then(() => {
                        toastService.info("krav slettet");
                        setTimeout(() => {
                            window.location.reload();
                        }, 250);
                    })
                .catch(error => toastService.error(error));
            }
        })
    }

}

/**
 * Validates a section number input according to business rules
 * @param {string} sectionValue - The section value to validate
 * @param {number} maxLength - Maximum allowed length (default: 10)
 * @param {number} maxInteger - Maximum allowed integer value (default: 2147483647)
 * @returns {object} - {isValid: boolean, errorMessage: string}
 */
function ValidateSection(sectionValue, maxLength, maxInteger) {
    // Basic length validation
    if (sectionValue.length === 0) {
        return {
            isValid: false,
            errorMessage: "Du skal angive et sektionsnummer"
        }
    } else if (sectionValue.length > maxLength) {
        return {
            isValid: false,
            errorMessage: "Der må maks angives 10 tegn"
        }
    } else {
        // Parse and validate structure
        const parts = sectionValue.split('.');

        // Check for valid number of parts (not more than three allowed, cases like "1.2.3.4" are not allowed)
        if (parts.length > 3) {
            return {
                isValid: false,
                errorMessage: "Maksimalt 3 niveauer tilladt (f.eks. 1.2.3)"
            }
        }

        // Validate each part
        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];

            // Check if part is empty (cases like "1..2" or ".1")
            if (part === '') {
                return {
                    isValid: false,
                    errorMessage: "Ugyldigt format (Du må ikke bruge et format som 1..2)"
                }
            }

            // Check that parts only contain digits (not letters or other special characters)
            if (!/^\d+$/.test(part)) {
                return {
                    isValid: false,
                    errorMessage: "Kun tal og punktum er tilladt"
                }
            }

            // Check that numerical value is in range
            const numericValue = parseInt(part, 10);
            if (numericValue > maxInteger) {
                return {
                    isValid: false,
                    errorMessage: "Tallet er for langt"
                }
            }
        }
    }

    // All validation passed
    return {
        isValid: true,
        errorMessage: ""
    };
}
