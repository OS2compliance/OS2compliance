let relationsChoice;
let taskViewChoiceElements = [];

function updateRelationsForDocument(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=TASK,SUPPLIER,ASSET,REGISTER,STANDARD_SECTION&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}

function initTaskDocumentRelationSelect() {
    const relationsSelect = document.getElementById('documentRelationSelect');
    if (relationsSelect == null) {
        return;
    }
    relationsChoice = initSelect(relationsSelect);
    updateDocumentRelations(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            updateDocumentRelations(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            updateDocumentRelations(relationsChoice, "");
        },
        false,
    );
}

var userChoicesEditSelect = null;
var ouChoicesEditSelect = null;
function loadViewAndEditForm() {
    userChoicesEditSelect = initUserSelect('userSelect');
    ouChoicesEditSelect = initOUSelect('ouSelect');

    userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoicesEditSelect);
    });

    taskViewChoiceElements.push(userChoicesEditSelect);

    document.querySelectorAll('.editField').forEach(elem => {
        elem.disabled = true;
    });
    ouChoicesEditSelect.disable();
    userChoicesEditSelect.disable();
}

function editMode(enabled) {
    if (enabled) {
        document.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = false;
        });
        ouChoicesEditSelect.enable();
        userChoicesEditSelect.enable();
        document.getElementById('saveEditTaskBtn').hidden = false;
        document.getElementById('editTaskBtn').hidden = true;
        document.getElementById('completeBtn').hidden = true;
    } else {
        document.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = true;
        });
        ouChoicesEditSelect.disable();
        userChoicesEditSelect.disable();
        document.getElementById('saveEditTaskBtn').hidden = true;
        document.getElementById('editTaskBtn').hidden = false;
        document.getElementById('completeBtn').hidden = false;
    }
}

function initRelationSelect() {
    const relationsSelect = document.getElementById('relationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    updateRelationsForDocument(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            updateRelationsForDocument(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            updateRelationsForDocument(relationsChoice, "");
        },
        false,
    );
}

function addRelationFormLoaded() {
    initRelationSelect();
}

function completeTaskFormLoaded() {
    initTaskDocumentRelationSelect();
}
