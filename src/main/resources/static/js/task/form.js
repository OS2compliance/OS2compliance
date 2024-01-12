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


const copyTaskService = new CopyTaskService();

function CopyTaskService() {
    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showCopyDialog = function(taskId) {
        let container = document.getElementById('copyTaskContainer');
        fetch(`${baseUrl}${taskId}/copy`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function() {
        this.modalContainer = document.getElementById('copyModal');

        let responsibleSelect = this.getScopedElementById('taskCopyUserSelect');
        if(responsibleSelect !== null) {
            responsibleSelect =  initUserSelect('taskCopyUserSelect');
        }

       let taskCopyOuSelect = this.getScopedElementById('taskCopyOuSelect');
       if(taskCopyOuSelect !== null) {
           taskCopyOuSelect = initOUSelect('taskCopyOuSelect');
       }

       let taskCopyRelationSelect = this.getScopedElementById('copyTaskRelationsSelect');
       if(taskCopyRelationSelect !== null) {
           copyTaskRelationsSelect = initCopyTaskRelationSelect();
       }

       let tagCopySelect = this.getScopedElementById('copyTaskTagsSelect');
       if(tagCopySelect !== null) {
            copyTaskTagsSelect = initTagSelect('copyTaskTagsSelect');
       }

        initFormValidationForForm("copyTaskModalForm",
            () => this.validate());

        this.copyTaskModal = new bootstrap.Modal(this.modalContainer);
        this.copyTaskModal.show();
    }

    this.validate = function() {
       /* let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        if (this.assetChoicesSelect != null) {
            result &= checkInputField(this.assetChoicesSelect, true);
        } else if (this.registerChoicesSelect != null) {
            result &= validateChoices(this.registerChoicesSelect);
        }*/
        return true ; //result
    }

    function initCopyTaskRelationSelect() {
       const relationsSelect = document.getElementById('copyTaskRelationsSelect');
       const relationsChoice = initSelect(relationsSelect);
       updateRelations(relationsChoice, "");
       relationsSelect.addEventListener("search",
           function(event) {
               updateRelations(relationsChoice, event.detail.value);
           },
           false,
       );
       relationsSelect.addEventListener("change",
           function(event) {
               updateRelations(relationsChoice, "");
           },
           false,
       );
   }

}

