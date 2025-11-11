import FormValidationService from "../FormValidationService.js";


export function validateFormBeforeSubmit (event, form) {
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
        setFieldValidity(nameInput, feedback, isValid);
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
        setFieldValidity(textArea, feedback, isValid);
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
        setFieldValidity(input, feedback, isValid);
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

function setFieldValidity (field, feedback, isValid) {
    if (isValid) {
        field.classList.remove("is-invalid");
        if (feedback) feedback.style.display = "none";
    } else {
        field.classList.add("is-invalid");
        if (feedback) feedback.style.display = "block";
    }
};