import FormValidationService from "../FormValidationService.js";


export function validateFormBeforeSubmit (event, form) {
    let valid = true;
    let invalidFields = [];

    // When saving as draft, obligatory fields are allowed to be empty
    const isDraft = event.submitter != null && event.submitter.dataset.draft === 'true';
    const draftInput = form.querySelector('input[name="draft"]');
    if (draftInput) {
        draftInput.value = isDraft;
    }

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
    if (!isDraft && !fvs.validate_isNotEmpty()) {
        valid = false;
    }

    if (!valid) {
        event.preventDefault();
        if (invalidFields.length > 0) {
            invalidFields[0].scrollIntoView({behavior: 'smooth', block: 'center'});
            invalidFields[0].focus();
        }
    } else {
        lockSubmitButtons(form);
    }
};

const lockedButtons = new Set();
const UNLOCK_AFTER_MS = 15000;

function unlockSubmitButtons () {
    lockedButtons.forEach(button => button.disabled = false);
    lockedButtons.clear();
}

// Covers a page restored from the bfcache, where the buttons come back disabled.
window.addEventListener('pageshow', unlockSubmitButtons);

/** Clicking Gem again while the post is in flight starts another one, and each creates an incident. */
export function lockSubmitButtons (form) {
    new Set([...document.querySelectorAll(`[form="${form.id}"]`), ...form.querySelectorAll('button, input')])
        .forEach(element => {
            if (element.type === 'submit') {
                element.disabled = true;
                lockedButtons.add(element);
            }
        });
    // When the navigation never happens the form would otherwise sit unusable with the typed data in it.
    setTimeout(unlockSubmitButtons, UNLOCK_AFTER_MS);
}

function setFieldValidity (field, feedback, isValid) {
    if (isValid) {
        field.classList.remove("is-invalid");
        if (feedback) feedback.style.display = "none";
    } else {
        field.classList.add("is-invalid");
        if (feedback) feedback.style.display = "block";
    }
};