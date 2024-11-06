
let userChoicesEditSelect = null;

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function loadViewAndEditForm() {
    initDatepicker("#nextRevisionBtn", "#nextRevision");
    userChoicesEditSelect = choiceService.initUserSelect("userSelect");

    userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoicesEditSelect);
    });
    document.querySelectorAll('.editField').forEach(elem => {
      elem.disabled = true;
    });
    userChoicesEditSelect.disable();

    initFormValidationForForm("editForm", () => validateChoices(userChoicesEditSelect));
}

function editMode(enabled) {
    if (enabled) {
        document.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = false;
        });
        userChoicesEditSelect.enable();
        document.getElementById('saveEditBtn').hidden = false;
        document.getElementById('editBtn').hidden = true;
        document.querySelector('.clickableDocLink').style.display = 'none'
        document.querySelector('.editableDocLink').style.display = ''
    } else {
        document.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = true;
        });
        userChoicesEditSelect.disable();
        document.getElementById('saveEditBtn').hidden = true;
        document.getElementById('editBtn').hidden = false;
                document.querySelector('.clickableDocLink').style.display = ''
                document.querySelector('.editableDocLink').style.display = 'none'
    }
}

function addRelationFormLoaded() {
    choiceService.initDocumentRelationSelect();
}
