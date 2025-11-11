import initTagSelect from "../tags/tag-selector.js";

const createTaskService = new CreateTaskService();
const taskLinkService = new TaskLinkService();
const subTaskLinkService = new SubTaskLinkService();
window.createTaskService = createTaskService;
window.taskLinkService = taskLinkService;
window.subTaskLinkService = subTaskLinkService;

document.addEventListener('DOMContentLoaded', (e) => {
    // Find create task button ( if it exists) and add event listener
    const createTaskButton = document.getElementById('createTaskButton');
    if (createTaskButton) {
        createTaskButton.addEventListener('click', (e) => {
            createTaskService.show()
        })
    }
    subTaskLinkService.init();
})

function SubTaskLinkService() {
    this.init = function() {
        const addBtn = document.getElementById('addSubTaskBtn');
        const copyAddBtn = document.getElementById('copyAddSubTaskBtn');

        if (addBtn) {
            addBtn.addEventListener('click', () => this.addSubTask());
        }

        if (copyAddBtn) {
            copyAddBtn.addEventListener('click', () => this.addSubTask());
        }

        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('subtask-remove-btn')) {
                this.removeSubTask(e.target);
            }
        });

        document.addEventListener('input', (e) => {
            if (e.target.matches('#subTasksContainer input[type="text"]') ||
                e.target.matches('#subTaskEditContainer input[type="text"]')) {
                this.validateSubTaskInput(e.target);
            }
        });

        this.validateExistingInputs();
    }

    this.validateExistingInputs = function() {
        const containers = ['subTasksContainer', 'subTaskEditContainer'];
        containers.forEach(containerId => {
            const container = document.getElementById(containerId);
            if (container) {
                const inputs = container.querySelectorAll('input[type="text"]');
                inputs.forEach(input => this.validateSubTaskInput(input));
            }
        });
    }

    this.validateSubTaskInput = function(input) {
        const value = input.value;
        const length = value.length;

        const existingFeedback = input.parentElement.querySelector('.invalid-feedback');
        if (existingFeedback) {
            existingFeedback.remove();
        }

        input.classList.remove('is-invalid', 'is-valid');

        if (length === 0) {
            return true;
        } else if (length > 255) {
            input.classList.add('is-invalid');
            const feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            feedback.textContent = `Maks ${255} tegn (${length}/${255})`;
            input.parentElement.appendChild(feedback);
            return false;
        } else {
            input.classList.add('is-valid');
            return true;
        }
    }

    this.validateAllSubTasks = function() {
        let isValid = true;
        const containers = ['subTasksContainer', 'subTaskEditContainer'];

        containers.forEach(containerId => {
            const container = document.getElementById(containerId);
            if (container) {
                const inputs = container.querySelectorAll('input[type="text"]');
                inputs.forEach(input => {
                    if (!this.validateSubTaskInput(input)) {
                        isValid = false;
                    }
                });
            }
        });

        return isValid;
    }

    this.addSubTaskFromView = function() {
        const container = document.getElementById("subTaskEditContainer");
        if (!container) return;

        const index = container.children.length;
        const element = this.createSubTaskElement(index, false);

        const input = element.querySelector('input[type="text"]');
        const button = element.querySelector('button');
        if (input) input.classList.add('editField');
        if (button) button.classList.add('editField');

        container.appendChild(element);
    }

    this.createSubTaskElement = function(index, disableCheckBox=true) {
        const div = document.createElement("div");
        div.className = "input-group mb-2";

        const inputGroupText = document.createElement("div");
        inputGroupText.className = "input-group-text";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.name = `subTasks[${index}].completed`;
        checkbox.className = "form-check-input mt-0 editField";
        checkbox.disabled = disableCheckBox;

        const input = document.createElement("input");
        input.type = "text";
        input.name = `subTasks[${index}].name`;
        input.className = "form-control";
        input.placeholder = "Indtast underopgave...";

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "btn btn-danger subtask-remove-btn";
        removeBtn.textContent = "-";

        inputGroupText.appendChild(checkbox);
        div.appendChild(inputGroupText);
        div.appendChild(input);
        div.appendChild(removeBtn);

        return div;
    }

    this.addSubTask = function() {
        const container = document.getElementById('subTasksContainer');
        if (!container) return;

        const index = container.children.length;
        const element = this.createSubTaskElement(index);
        container.appendChild(element);
    }

    this.removeSubTask = function(button) {
        if (button && button.parentElement) {
            const container = button.parentElement.parentElement;
            button.parentElement.remove();
            this.reindexSubTasks(container);
        }
    }

    this.reindexSubTasks = function(container) {
        if (!container) return;

        const children = container.children;

        for (let i = 0; i < children.length; i++) {
            const checkbox = children[i].querySelector('input[type="checkbox"]');
            const textInput = children[i].querySelector('input[type="text"]');

            if (checkbox) {
                checkbox.name = `subTasks[${i}].completed`;
            }
            if (textInput) {
                textInput.name = `subTasks[${i}].name`;
            }
        }
    }
}

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
        this.notificationSelectHandler = initNotificationSelect(
            'taskNotificationSetting',
            'taskNotificationSelectDiv',
            'taskNotificationSelectInput'
        );
        this.createTaskDepartmentChoicesEditSelect.setChoices([{ value: '', label: 'Vælg forvaltning...', selected: true }], 'value', 'label', false);
        let templateDescriptionSelect = document.getElementById('taskCreateFormTemplateDescriptionSelect');

        let addSubTaskBtn = document.getElementById('addSubTaskBtn');
        addSubTaskBtn.addEventListener("click", () => subTaskLinkService.addSubTask())

        if (templateDescriptionSelect !== null) {
            new Choices(templateDescriptionSelect, {
                removeItemButton: true,
                searchEnabled: true,
                placeholderValue: 'Vælg en skabelon',
                searchPlaceholderValue: 'Søg...'
            });
            let previousDescription;

            templateDescriptionSelect.addEventListener('change', function(event) {
                let descriptionBox = document.getElementById('taskCreateFormdescription');
                if (templateDescriptionSelect.value !== '' && templateDescriptionSelect.value !== null) {
                    if (descriptionBox.value) {
                        previousDescription = descriptionBox.value;
                    }
                    descriptionBox.value = "";
                    descriptionBox.disabled = true;
                }
                else {
                    descriptionBox.value = previousDescription;
                    descriptionBox.disabled = false;
                }
            });
        }
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
            () => {
                const choicesValid = validateChoices(
                    this.createTaskUserChoicesEditSelect,
                    this.createTaskOuChoicesEditSelect
                );
                const subTasksValid = subTaskLinkService.validateAllSubTasks();

                return choicesValid && subTasksValid;
            }
        );
    }

    this.show = function(elem = null) {
        fetch(`/tasks/form`)
            .then(response => response.text()
                .then(data => {
                    this.taskModalDialog = document.getElementById('taskFormDialog');
                    this.taskModalDialog.innerHTML = data;
                    this.loaded();
                    this.initTaskRelationSelect();
                    initTagSelect('taskCreateFormTagsSelect');
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
