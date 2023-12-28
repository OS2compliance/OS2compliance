
let supplierChoices;
let userChoices;

function formReset() {
    const form = document.querySelector('form');
    form.reset();
    updateUsers(userChoices, "");
    updateSuppliers(supplierChoices, "");
}

function updateSuppliers(choices, search) {
    fetch( `/rest/suppliers/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(e => {
                    return {
                        value: e.id,
                        label: `${e.name}`}
                }), 'value', 'label', true);
            }))
        .catch(error => toastService.error(error));
}

function formLoaded() {
    initUserChoices();
    initSuppliersChoices();
    initFormValidationForFormChoicesOnly("createForm", () => validateChoices(userChoices));
}

function initUserChoices() {
    const userSelect = document.getElementById('userSelect');
    userChoices = initSelect(userSelect);
    updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    userChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoices);
    });
}

function initSuppliersChoices() {
    const supplierSelect = document.getElementById('supplierSelect');
    supplierChoices = initSelect(supplierSelect);
    updateSuppliers(supplierChoices, "");
    supplierSelect.addEventListener("search",
        function(event) {
            updateSuppliers(supplierChoices, event.detail.value);
        },
        false,
    );
    supplierChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(supplierChoices);
    });
}

function initResponsibleUsersOversightChoices() {
    const userSelect = document.getElementById('responsibleUserOversightSelect');
    userChoices = initSelect(userSelect);
    updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    userChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoices);
    });
}




// Choices js validation

