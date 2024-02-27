
copyTaskService = new CopyTaskService();
editTaskService = new EditTaskService();

document.addEventListener("DOMContentLoaded", function() {
    copyTaskService.init();
    editTaskService.init();
});

function EditTaskService() {
    this.taskModalDialog = null;
    this.init = function() {
    }

    this.showEditDialog = function(taskId) {
        fetch(`/tasks/form?id=${taskId}`)
            .then(response => response.text()
                .then(data => {
                    this.taskModalDialog = document.getElementById('editTaskDialog');
                    this.taskModalDialog.innerHTML = data;
                    this.onLoaded(taskId);
                }))
            .catch(error => { toastService.error(error); console.log(error)});
    }

    this.onLoaded = function(taskId, copy = false) {
        let self = this;
        initDatepicker("#taskEditFormTaskDeadlineBtn", "#taskEditFormTaskDeadline");
        this.editTaskOuChoicesSelect = initOUSelect('taskEditFormTaskOuSelect');

        this.editTaskUserChoicesEditSelect = initUserSelect('taskEditFormTaskUserSelect');
        this.editTaskUserChoicesEditSelect.passedElement.element.addEventListener('addItem', function() {
            var userUuid = self.editTaskUserChoicesEditSelect.passedElement.element.value;
            fetch( `/rest/ous/user/` + userUuid).then(response =>  response.text().then(data => {
                self.editTaskOuChoicesSelect.setChoiceByValue(data);
            })).catch(error => toastService.error(error));
        })

        this.editTaskUserChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.editTaskUserChoicesEditSelect);
        });
        initFormValidationForForm('taskEditForm',
            () => validateChoices(
                this.editTaskUserChoicesEditSelect, this.editTaskOuChoicesSelect));

        this.taskModalDialog.querySelector('#taskEditFormThreatAssessmentExplainer').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormRelationsDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormTagsDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormTaskRiskId').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRiskCustomId').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRiskCatalogIdentifier').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRepetitionDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormBtnCheck').disabled = true;
        this.taskModalDialog.querySelector('#taskEditFormBtnTask').disabled = true;

        const editTaskModal = new bootstrap.Modal(this.taskModalDialog);
        editTaskModal.show();
    }

}

function CopyTaskService() {
    this.modalContainer = null;

    this.init = function() {
    }

    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showCopyDialog = function(taskId) {
        let container = document.getElementById('copyTaskContainer');
        fetch(`${baseUrl}${taskId}/copy`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onLoaded();
                })
            )
            .catch(error => { toastService.error(error); console.log(error)});
    }

    this.onLoaded = function() {
        this.modalContainer = document.getElementById('copyModal');
        initDatepicker("#copyTaskDeadlineBtn", "#copyTaskDeadline");

        let responsibleSelect = this.getScopedElementById('copyTaskUserSelect');
        if(responsibleSelect !== null) {
            initUserSelect('copyTaskUserSelect');
        }

       let copyTaskOuSelect = this.getScopedElementById('copyTaskOuSelect');
       if(copyTaskOuSelect !== null) {
           initOUSelect('copyTaskOuSelect');
       }

       let copyTaskRelationSelect = this.getScopedElementById('copyTaskRelationsSelect');
       if(copyTaskRelationSelect !== null) {
           this.initCopyTaskRelationSelect();
       }

       let tagCopySelect = this.getScopedElementById('copyTaskTagsSelect');
       if(tagCopySelect !== null) {
            initTagSelect('copyTaskTagsSelect');
       }

        initFormValidationForForm("copyTaskModalForm");

        this.copyTaskModal = new bootstrap.Modal(this.modalContainer);
        this.copyTaskModal.show();
    }

    this.initCopyTaskRelationSelect = function() {
        const relationsSelect = document.getElementById('copyTaskRelationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        updateRelations(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                updateRelations(relationsChoice, event.detail.value);
            },
           false
        );
        relationsSelect.addEventListener("change",
            function() {
                updateRelations(relationsChoice, "");
            },
            false
       );
   }

   this.selectTaskOption = function(value) {
       const form = document.querySelector('#copyTaskModalForm');
       const repetitionField = form.querySelector('#copyTaskRepetition');
       if (value === 'TASK') {
           repetitionField.value = 'NONE';
       }
       repetitionField.disabled = value !== 'CHECK';
   }

}

