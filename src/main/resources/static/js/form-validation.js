
function validateChoices(...choiceList) {
    let result = true;
    for (let i = 0; i < choiceList.length; i++) {
        if (!checkInputField(choiceList[i])) {
            result = false;
        }
    }
    return result;
}

function initFormValidationForForm(formId, extraValidationFunction) {
    const form = document.getElementById(formId);
    form.addEventListener( "submit", e => {
        if (extraValidationFunction !== undefined && !extraValidationFunction()) {
            e.preventDefault()
            e.stopPropagation()
        }

        if ( !form.checkValidity()) {
            e.preventDefault()
            e.stopPropagation()
        }
        form.classList.add( "was-validated" )
    }, false)
}

function initFormValidationForFormChoicesOnly(formId, extraValidationFunction) {
    const form = document.getElementById(formId)
    form.addEventListener("submit", e => {
    if(!extraValidationFunction()) {
        e.preventDefault()
        e.stopPropagation()
    }
    form.classList.add( "was-validated" )
    }, false)
}
