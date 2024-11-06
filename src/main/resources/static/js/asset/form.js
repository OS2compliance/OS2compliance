
let supplierChoices;
let userChoices;

function formReset() {
    const form = document.querySelector('form');
    form.reset();
    choiceService.updateUsers(userChoices, "");
    choiceService.updateSuppliers(supplierChoices, "");
}

function formLoaded() {
    initUserChoices();
    initSuppliersChoices();
    initFormValidationForFormChoicesOnly("createForm", () => validateChoices(userChoices));
}

function initUserChoices() {
    const userSelect = document.getElementById('userSelect');
    userChoices = initSelect(userSelect);
    choiceService.updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            choiceService.updateUsers(userChoices, event.detail.value);
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
    choiceService.updateSuppliers(supplierChoices, "");
    supplierSelect.addEventListener("search",
        function(event) {
            choiceService.updateSuppliers(supplierChoices, event.detail.value);
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
    choiceService.updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            choiceService.updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    userChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoices);
    });
}




// Choices js validation

