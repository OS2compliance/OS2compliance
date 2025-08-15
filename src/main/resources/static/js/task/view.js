

let viewTaskService = new ViewTaskService();
document.addEventListener("DOMContentLoaded", function(event) {
    viewTaskService.init();
});

function ViewTaskService() {
    this.userChoicesEditSelect = null;
    this.ouChoicesEditSelect = null;
    this.nameField = null;

    this.init = function() {

        this.loadViewAndEditForm();
        this.initRelationSelect();
        this.initTaskDocumentRelationSelect();
        choiceService.initTagSelect("tagsSelect");
        initFormValidationForForm('editForm');
        initFormValidationForForm('completeTaskForm');
        initDatepicker("#deadlineBtn", "#deadline");

        var textarea = document.getElementById('description');
        if (textarea) {
            this.fitDescription(textarea);
            textarea.addEventListener('input', function () {
                this.fitDescription(this);
            });
        }
    }

    // In case this task is an oversight, a special oversight dialog can be shown
    this.showOversightDialog = (source) => {
        const assetId = source.dataset.assetId;
        const url = `/assets/oversight/${assetId}/asset`;
        oversightService.initOversightModal(null, 'asset', assetId)
            .then(() => {oversightDialog.show()});
    }

    this.fitDescription = function (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = textarea.scrollHeight + 'px';
    }

    this.setEditMode = function(enabled) {
        if (enabled) {
            document.querySelectorAll('.editField').forEach(elem => {
                elem.disabled = false;
            });
            this.ouChoicesEditSelect.enable();
            this.userChoicesEditSelect.enable();
            document.getElementById('saveEditTaskBtn').hidden = false;
            document.getElementById('editTaskBtn').hidden = true;
            document.getElementById('completeBtn').hidden = true;
            document.getElementById('realLink').hidden = true;
            document.getElementById('linkField').hidden = false;
            this.nameField.disabled = false
        } else {
            document.querySelectorAll('.editField').forEach(elem => {
                elem.disabled = true;
            });
            this.ouChoicesEditSelect.disable();
            this.userChoicesEditSelect.disable();
            document.getElementById('saveEditTaskBtn').hidden = true;
            document.getElementById('editTaskBtn').hidden = false;
            document.getElementById('completeBtn').hidden = false;
            document.getElementById('realLink').hidden = false;
            document.getElementById('linkField').hidden = true;
            this.nameField.disabled = true
        }
    }

    this.loadViewAndEditForm = function() {
        const self = this;
        this.userChoicesEditSelect = choiceService.initUserSelect('userSelect');
        this.ouChoicesEditSelect = choiceService.initOUSelect('ouSelect');
        this.nameField = document.getElementById("taskNameField")

        this.userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.userChoicesEditSelect);
        });

        document.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = true;
        });
        this.ouChoicesEditSelect.disable();
        this.userChoicesEditSelect.disable();
    }

    this.initRelationSelect = function() {
        const self = this;
        const relationsSelect = document.getElementById('relationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        this.updateRelationsForTask(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                self.updateRelationsForTask(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function() {
                self.updateRelationsForTask(relationsChoice, "");
            },
            false,
        );
    }

    this.initTaskDocumentRelationSelect = function() {
        const relationsSelect = document.getElementById('documentRelationSelect');
        if (relationsSelect == null) {
            return;
        }
        const relationsChoice = initSelect(relationsSelect);
        choiceService.updateDocumentRelations(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                choiceService.updateDocumentRelations(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function() {
                choiceService.updateDocumentRelations(relationsChoice, "");
            },
            false,
        );
    }


    this.updateRelationsForTask = function (choices, search) {
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
            .catch(defaultErrorHandler);
    }
}
