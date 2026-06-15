
let copyTaskService = new CopyTaskService();
let editTaskService = new EditTaskService();
let initTagSelect;

document.addEventListener("DOMContentLoaded", async function() {
    initTagSelect = await import("../tags/tag-selector.js"); // dynamic import. replace with regular import as soon as possible
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
            .catch(error => { toastService.error(error); console.error(error)});
    }

    this.onLoaded = function(taskId, copy = false) {
        let self = this;
        initDatepicker("#taskEditFormTaskDeadlineBtn", "#taskEditFormTaskDeadline");
        this.editTaskOuChoicesSelect = choiceService.initOUSelect('taskEditFormTaskOuSelect');
        this.editTaskDepartmentChoicesSelect = choiceService.initOUSelect('taskEditFormTaskDepartmentSelect');

        this.editTaskUserChoicesEditSelect = choiceService.initUserSelect('taskEditFormTaskUserSelect');
        this.editTaskUserChoicesEditSelect.passedElement.element.addEventListener('addItem', function() {
            var userUuid = self.editTaskUserChoicesEditSelect.passedElement.element.value;
            fetch( `/rest/ous/user/` + userUuid).then(response =>  {
                if (response.status === 204) {
                    return;
                }

                if (response.ok) {
                    response.json().then(orgUnit => {
                        self.editTaskOuChoicesSelect.setChoiceByValue(orgUnit.uuid);
                    })
                }
            }).catch(error => toastService.error(error));
        })

        this.editTaskUserChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.editTaskUserChoicesEditSelect);
        });

        this.editTaskUserChoicesEditSelect.passedElement.element.addEventListener('removeItem', function() {
            self.editTaskOuChoicesSelect.removeActiveItems();
        });

        this.initSubTaskButtons();

        subTaskLinkService.validateExistingInputs();

        initFormValidationForForm('taskEditForm',
            () => {
                const choicesValid = validateChoices(
                    this.editTaskUserChoicesEditSelect,
                    this.editTaskOuChoicesSelect
                );
                const subTasksValid = subTaskLinkService.validateAllSubTasks();

                return choicesValid && subTasksValid;
            }
        );

        this.taskModalDialog.querySelector('#taskEditFormThreatAssessmentExplainer').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormRelationsDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormTagsDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormTaskRiskId').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRiskCustomId').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRiskCatalogIdentifier').value = null;
        this.taskModalDialog.querySelector('#taskEditFormRepetitionDiv').style.display = 'none';
        this.taskModalDialog.querySelector('#taskEditFormBtnCheck').disabled = true;
        this.taskModalDialog.querySelector('#taskEditFormBtnTask').disabled = true;

        const editTaskModal = new bootstrap.Modal(this.taskModalDialog, { backdrop: 'static' });
        editTaskModal.show();
    }

    this.initSubTaskButtons = function() {
        const addBtn = document.getElementById('addSubTaskBtn');
        if (addBtn) {
            // Fjern gamle event listeners ved at clone noden
            const newAddBtn = addBtn.cloneNode(true);
            addBtn.parentNode.replaceChild(newAddBtn, addBtn);

            newAddBtn.addEventListener('click', () => {
                subTaskLinkService.addSubTask();
            });
        }
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
            .catch(error => { toastService.error(error); console.error(error)});
    }

    this.onLoaded = function() {
        this.modalContainer = document.getElementById('copyModal');
        initDatepicker("#copyTaskDeadlineBtn", "#copyTaskDeadline");

        let responsibleSelect = this.getScopedElementById('copyTaskUserSelect');
        if(responsibleSelect !== null) {
            choiceService.initUserSelect('copyTaskUserSelect');
        }

       let copyTaskOuSelect = this.getScopedElementById('copyTaskOuSelect');
       if(copyTaskOuSelect !== null) {
           choiceService.initOUSelect('copyTaskOuSelect');
       }

       let copyTaskRelationSelect = this.getScopedElementById('copyTaskRelationsSelect');
       if(copyTaskRelationSelect !== null) {
           this.initCopyTaskRelationSelect();
       }

        let addSubTaskBtn = document.getElementById('copyAddSubTaskBtn');
        if (addSubTaskBtn !== null) {
            addSubTaskBtn.addEventListener("click", () => subTaskLinkService.addSubTask())
        }

        let removeSubTaskBtn = document.getElementById('copyRemoveSubTaskBtn');
        if (removeSubTaskBtn !== null) {
            removeSubTaskBtn.addEventListener("click", () => subTaskLinkService.removeSubTask())
        }

       let tagCopySelect = this.getScopedElementById('copyTaskTagsSelect');
       if(tagCopySelect !== null) {
           //initTagSelect('copyTaskTagsSelect');
       }

        initFormValidationForForm("copyTaskModalForm", () => subTaskLinkService.validateAllSubTasks());
        this.notificationSelectHandler = initNotificationSelect(
            'copyTaskNotificationSetting',
            'copyTaskNotificationSelectDiv',
            'copyTaskNotificationSelectInput'
        );


        this.copyTaskModal = new bootstrap.Modal(this.modalContainer, { backdrop: 'static' });
        this.copyTaskModal.show();
    }

    this.initCopyTaskRelationSelect = function() {
        const relationsSelect = document.getElementById('copyTaskRelationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelations(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                choiceService.updateRelations(relationsChoice, event.detail.value);
            },
           false
        );
        relationsSelect.addEventListener("change",
            function() {
                choiceService.updateRelations(relationsChoice, "");
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

