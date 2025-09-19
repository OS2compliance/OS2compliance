const emptyFieldHandlers = {
    'TEXTAREA': (element) => textAreaEmpty(element),
    'INPUT': (element) => inputEmpty(element),
    'SELECT': (element) => selectEmpty(element),
}

const FVSCLASS = {
    ERRORMESSAGE: '"fvs-validation-error"',
    NOTEMPTY: "fvs-not-empty"
}

/**
 * Service class offering validation on a specific form
 */
export default class FormValidationService {
    form

    /**
     * Instantiates a new validation service for the specified form
     * @param form the form to validate
     */
    constructor(form) {
        if (!form || form.nodeName !== 'FORM') {
            throw new Error('FormValidationService must be instantiated with a Form to validate')
        }
        this.form = form;
    }

    /**
     * Checks emptyness of all elements in the form containing 'fvs-not-empty' class, showing an error message after each
     * @returns {boolean} false if any of the marked elements is empty. True otherwise
     */
    validate_isNotEmpty() {
        const fields = [...this.form.getElementsByClassName(FVSCLASS.NOTEMPTY)]

        const notEmptyFields = fields.filter(field => {
            return emptyFieldHandlers[field.nodeName](field);
        })

        notEmptyFields.forEach(field => {
            showErrorForField(field, 'Dette felt må ikke være tomt')
        })

        return notEmptyFields.length < 1;
    }

    /**
     * Removes all validation messages provided by this class
     */
    removeValidationMessages() {
        [...this.form.getElementsByClassName(FVSCLASS.ERRORMESSAGE)].forEach(errorMessage => errorMessage.remove());
    }

};

/**
 * Creates an error message element after the given field, with the given message
 * @param field the field with error
 * @param message the message for the error message element
 */
function showErrorForField(field, message) {
    if (!field) {
        return;
    }
    // Mark field invalid
    field.classList.add('is-invalid');

    // Show error message element
    const errorElement = document.createElement("div");
    errorElement.classList.add(FVSCLASS.ERRORMESSAGE);
    errorElement.classList.add("invalid-feedback");
    errorElement.textContent = message;

    if (field.nextSibling) {
        field.parentNode.insertBefore(errorElement, field.nextSibling);
    } else {
        field.parentNode.appendChild(errorElement);
    }
}

/**
 * Checks if a textarea is empty
 * @param textArea element to check
 * @returns {boolean} true if empty, false otherwise
 */
function textAreaEmpty(textArea) {
    const value = textArea.value;
    return !value || value === '' || value.length < 1;

}

/**
 * Checks if an input element is empty
 * @param input element to check
 * @returns {boolean} true if empty, false otherwise
 */
function inputEmpty(input) {
    const value = input.value;
    return !value || value === '' || value.length < 1;
}

/**
 * Checks if a select element is empty
 * @param select element to check
 * @returns {boolean} true if empty, false otherwise
 */
function selectEmpty(select) {
    const value = select.value;
    return !value || value === '' || value.length < 1 || select.selectedOptions.length < 1;
}