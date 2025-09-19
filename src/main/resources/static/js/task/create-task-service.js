
const createTaskService = new CreateTaskService();
const taskLinkService = new TaskLinkService();

document.addEventListener('DOMContentLoaded', (e) => {
    // Find create task button ( if it exists) and add event listener
    const createTaskButton = document.getElementById('createTaskButton');
    if (createTaskButton) {
        createTaskButton.addEventListener('click', (e) => {
            createTaskService.show()
        })
    }
})

function TaskLinkService() {
    this.addLink = function() {
        const container = document.getElementById("linksEditContainer");
        const index = container.children.length;

        const div = document.createElement("div");
        div.className = "input-group mb-2";

        const input = document.createElement("input");
        input.type = "text";
        input.name = `links[${index}].url`;
        input.className = "form-control editField";

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "btn btn-danger editField";
        removeBtn.textContent = "-";
        removeBtn.addEventListener("click", function () {
            taskLinkService.removeLink(removeBtn);
        });

        div.appendChild(input);
        div.appendChild(removeBtn);
        container.appendChild(div);
    }

    this.removeLink = function(btn) {
        const div = btn.parentNode;
        div.remove();
        taskLinkService.reindexLinks();
    }

    this.reindexLinks = function() {
        const container = document.getElementById("linksEditContainer");
        const children = container.children;

        for (let i = 0; i < children.length; i++) {
            const input = children[i].querySelector('input[name^="links"]');
            if (input) {
                input.name = `links[${i}].url`;
            }
        }
    }
}

function CreateTaskService() {
    this.taskModalDialog = null;
    this.createTaskOuChoicesEditSelect = null;
    this.createTaskDepartmentChoicesEditSelect = null;

    this.selectCreateTaskOption = function(value) {
        const form = document.querySelector('#taskCreateForm');
        const repetitionField = form.querySelector('#taskCreateFormRepetition');
        if (value === 'TASK') {
            repetitionField.value = 'NONE';
        }
        repetitionField.disabled = value !== 'CHECK';
    }

    this.loaded = function() {
        let self = this;
        this.selectCreateTaskOption('TASK');
        initDatepicker("#taskCreateFormTaskDeadlineBtn", "#taskCreateFormTaskDeadline");
         this.createTaskOuChoicesEditSelect = choiceService.initOUSelect('taskCreateFormTaskOuSelect');
         this.createTaskDepartmentChoicesEditSelect = choiceService.initOUSelect('taskCreateFormTaskDepartmentSelect');

        this.createTaskUserChoicesEditSelect = choiceService.initUserSelect('taskCreateFormTaskUserSelect');
        this.createTaskUserChoicesEditSelect.passedElement.element.addEventListener('addItem', function() {
             var userUuid = self.createTaskUserChoicesEditSelect.passedElement.element.value;
             fetch( `/rest/ous/user/` + userUuid).then(response =>  response.text().then(data => {
                self.createTaskOuChoicesEditSelect.setChoiceByValue(data);
             })).catch(error => toastService.error(error));
        })


        this.createTaskUserChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.createTaskUserChoicesEditSelect);
        });
        initFormValidationForForm('taskCreateForm',
            () => validateChoices(
                this.createTaskUserChoicesEditSelect, this.createTaskOuChoicesEditSelect));
    }

    this.show = function(elem = null) {
        fetch(`/tasks/form`)
            .then(response => response.text()
                .then(data => {
                    this.taskModalDialog = document.getElementById('taskFormDialog');
                    this.taskModalDialog.innerHTML = data;
                    this.loaded();
                    this.initTaskRelationSelect();
                    choiceService.initTagSelect('taskCreateFormTagsSelect');
                    // create task modal - explainer and riskId
                    // if elem != null it means that the method is called from the risk view page
                    if (elem != null) {
                        var riskId = elem.dataset.riskid;
                        var customId = elem.dataset.customid;
                        var catalogIdentifier = elem.dataset.catalogidentifier;
                        this.taskModalDialog.querySelector('#taskCreateFormThreatAssessmentExplainer').style.display = '';
                        this.taskModalDialog.querySelector('#taskCreateFormTaskRiskId').value = riskId;
                        this.taskModalDialog.querySelector('#taskCreateFormRiskCustomId').value = customId;
                        this.taskModalDialog.querySelector('#taskCreateFormRiskCatalogIdentifier').value = catalogIdentifier;
                    } else {
                        this.taskModalDialog.querySelector('#taskCreateFormThreatAssessmentExplainer').style.display = 'none';
                        this.taskModalDialog.querySelector('#taskCreateFormTaskRiskId').value = null;
                        this.taskModalDialog.querySelector('#taskCreateFormRiskCustomId').value = null;
                        this.taskModalDialog.querySelector('#taskCreateFormRiskCatalogIdentifier').value = null;
                    }

                    const createTaskModal = new bootstrap.Modal(this.taskModalDialog);
                    createTaskModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.initTaskRelationSelect = function() {
        const relationsSelect = document.getElementById('taskCreateFormRelationsSelect');
        const relationsChoice = initSelect(relationsSelect);
        choiceService.updateRelations(relationsChoice, "");
        relationsSelect.addEventListener("search",
            function(event) {
                choiceService.updateRelations(relationsChoice, event.detail.value);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function(event) {
                choiceService.updateRelations(relationsChoice, "");
            },
            false,
        );
    }

}
