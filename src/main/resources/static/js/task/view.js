import OnUnSubmittedService from "../on-unsubmitted-changes-service.js";
import initRelatedTagList from "../tags/related-tag-list.js";

let onUnSubmittedService = new OnUnSubmittedService();
let viewTaskService = new ViewTaskService();
document.addEventListener("DOMContentLoaded", function(event) {
    viewTaskService.init();
    initRelatedTagList('#editForm')
});

function ViewTaskService() {
    this.userChoicesEditSelect = null;
    this.ouChoicesEditSelect = null;
    this.ouDepartmentChoicesEditSelect = null;
    this.nameField = null;

    this.init = function() {
        const oversightBtn = document.getElementById("oversightBtn");
        const saveEditTaskBtn = document.getElementById("saveEditTaskBtn");
        const editTaskBtn = document.getElementById("editTaskBtn");

        oversightBtn?.addEventListener("click", () => {
            this.showOversightDialog(oversightBtn.dataset.assetId);
        });

        editTaskBtn?.addEventListener("click", () => {
            this.setEditMode(true);
            // Prevent user from reloading/leaving page without confirmation
            onUnSubmittedService.setChangesMade();
        });

        saveEditTaskBtn?.addEventListener("click", () => {
            onUnSubmittedService.reset();
        });
        this.loadViewAndEditForm();
        this.initRelationSelect();
        this.initTaskDocumentRelationSelect();
        this.loadDescriptionTemplateSelect();
        this.initSubTaskBtns();


        initFormValidationForForm('editForm', () => subTaskLinkService.validateAllSubTasks());

        if (taskType === 'CHECK') {
            initFormValidationForForm('completeTaskForm', () => {
                const comment = document.getElementById("completionComment");
                const taskResultSelect = document.getElementById("taskResultSelect");
                const taskType = document.getElementById("taskType");

                const subTasksValid = this.validateSubTasksCompletion();

                let commentValid = false;
                if (taskResultSelect.value === 'NO_ERROR') {
                    comment.classList.remove('is-invalid');
                    commentValid = true;
                } else if (taskResultSelect.value !== 'NO_ERROR' && comment.value.trim()) {
                    comment.classList.remove('is-invalid');
                    commentValid = true;
                } else {
                    comment.classList.add('is-invalid');
                    commentValid = false;
                }

                return subTasksValid && commentValid;
            });
        } else {
            initFormValidationForForm('completeTaskForm', () => {
                const comment = document.getElementById("completionComment");

                const subTasksValid = this.validateSubTasksCompletion();

                let commentValid = false;
                if (!comment.value) {
                    comment.classList.add('is-invalid');
                    commentValid = false;
                } else {
                    comment.classList.remove('is-invalid');
                    commentValid = true;
                }

                return subTasksValid && commentValid;
            });
        }

        initDatepicker("#deadlineBtn", "#deadline");
        initDatepicker("#startDateBtn", "#startDate");
        initDatepicker("#TaskDeadlineBtn", "#TaskDeadline");
        let taskDeadline = document.querySelector("#TaskDeadline");
        if (taskDeadline) {
            taskDeadline.value = new Date().toLocaleDateString('da-DK', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            }).replace(/\./g, '/').replace(/(\d{2})\/(\d{2})\/(\d{4})/, '$1/$2-$3');
        }
        var textarea = document.getElementById('description');
        if (textarea) {
            this.fitDescription(textarea);
            textarea.addEventListener('input', function () {
                this.fitDescription(this);
            });
        }

        this.notificationSelectHandler = initNotificationSelect(
            'viewTaskNotificationSetting',
            'viewTaskNotificationSelectDiv',
            'viewTaskNotificationSelectInput'
        );

        this.initInProgressNoteToggle();
        this.initCompleteAndStay();
    }

    this.initCompleteAndStay = function() {
        const completeStayUrl = '/tasks/complete/stay';
        const form = document.getElementById('completeTaskForm');
        const completeAndStayBtn = document.getElementById('completeAndStayBtn');

        if (!form || !completeAndStayBtn) {
            return;
        }

        const token = document.getElementsByName('_csrf')[0].getAttribute('content');

        form.addEventListener('submit', event => {
            if (event.submitter !== completeAndStayBtn || event.defaultPrevented) {
                return;
            }

            event.preventDefault();

            fetch(completeStayUrl, { method: 'POST', body: new FormData(form), headers: { 'X-CSRF-TOKEN': token } })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`${response.status} ${response.statusText}`);
                    }

                    return response.text();
                })
                .then(redirectUrl => {
                    window.location.href = redirectUrl;
                })
                .catch(defaultErrorHandler);
        });
    }

    this.initInProgressNoteToggle = function() {
        const inProgressCheckbox = document.getElementById('inProgress');
        const noteRow = document.getElementById('noteRow');

        if (!inProgressCheckbox || !noteRow) {
            return;
        }

        inProgressCheckbox.addEventListener('change', (event) => {
            noteRow.hidden = !event.target.checked;
        });
    }

    // In case this task is an oversight, a special oversight dialog can be shown
    this.showOversightDialog = (assetId) => {
        if(!assetId) {
            return;
        }

        const url = `/assets/oversight/${assetId}/asset`;
        oversightService.initOversightModal(null, 'asset', assetId)
            .then(() => {oversightDialog.show()});
    }

    this.initSubTaskBtns = function () {
        let addBtn = document.getElementById('subTaskAddLinkBtn');
        addBtn.addEventListener('click', () => subTaskLinkService.addSubTaskFromView());
    }

    this.validateSubTasksCompletion = function() {
        const subTaskCheckboxes = document.querySelectorAll('#completeTaskForm input[name="subTasksCompleted"]');

        // If there are no subtasks, validation passes
        if (subTaskCheckboxes.length === 0) {
            return true;
        }

        // Check if all subtasks are checked
        const allChecked = Array.from(subTaskCheckboxes).every(checkbox => checkbox.checked);

        const errorMessageDiv = document.getElementById('subTaskValidationError');

        if (!allChecked) {
            // Show error message
            if (!errorMessageDiv) {
                const errorDiv = document.createElement('div');
                errorDiv.id = 'subTaskValidationError';
                errorDiv.className = 'alert alert-danger mt-2';
                errorDiv.textContent = 'Alle underopgaver skal være fuldført før opgaven kan afsluttes.';

                const subTaskContainer = document.querySelector('#completeTaskForm .border.rounded.p-3.bg-light');
                if (subTaskContainer) {
                    subTaskContainer.parentElement.appendChild(errorDiv);
                }
            }
            return false;
        } else {
            // Remove error message if it exists
            if (errorMessageDiv) {
                errorMessageDiv.remove();
            }
            return true;
        }
    }

    this.fitDescription = function (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = textarea.scrollHeight + 'px';
    }

    // Feltet viser skabelonens tekst når en skabelon er valgt, så det låses - ellers ville den tekst blive
    // gemt oven i opgavens egen beskrivelse
    this.syncDescriptionLock = function() {
        const select = document.getElementById('taskDescriptionTemplateSelect');
        const descriptionField = document.getElementById('description');
        if (select === null || descriptionField === null) {
            return;
        }
        descriptionField.disabled = select.value !== '';
    }

    this.loadDescriptionTemplateSelect = function() {
        const select = document.getElementById('taskDescriptionTemplateSelect');
        const descriptionField = document.getElementById('description');
        let ownDescription = descriptionField.dataset.ownDescription || '';

        select.addEventListener("change", async function () {
            const selectedValue = this.value;

            // If "Ingen valgt" (no selection) or empty value
            if (!selectedValue || selectedValue === '') {
                descriptionField.value = ownDescription;
                descriptionField.disabled = false;
                return;
            }

            // Save current description before replacing it
            if (!descriptionField.disabled) {
                ownDescription = descriptionField.value;
            }

            const response = await fetch(`/rest/choicelists/custom/choiceValue/${selectedValue}`);
            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    descriptionField.value = data.description;
                    descriptionField.disabled = true;
                } else {
                    toastService.error("Kunne ikke hente beskrivelse");
                }
            } else {
                toastService.error("Der opstod en teknisk fejl");
            }
        });
    }

    this.setEditMode = function(enabled) {
        let performButton = document.getElementById('completeBtn') || document.getElementById('oversightBtn');
        if (enabled) {
            document.querySelectorAll('.editField').forEach(elem => {
                elem.disabled = false;
            });
            this.ouChoicesEditSelect.enable();
            this.ouDepartmentChoicesEditSelect.enable();
            this.userChoicesEditSelect.enable();
            document.getElementById('saveEditTaskBtn').hidden = false;
            document.getElementById('editTaskBtn').hidden = true;
            performButton.hidden = true;
            this.nameField.disabled = false
            this.syncDescriptionLock();
            document.getElementById("linksViewContainer").hidden = true;
            document.getElementById("subTaskViewContainer").hidden = true;
            document.getElementById("linksEditContainer").hidden = false;
            document.getElementById("subTaskEditContainer").hidden = false;
            document.getElementById("addLinkBtn").hidden = false;
            this.notificationSelectHandler.enable();
            document.getElementById("subTaskAddLinkBtn").hidden = false;
            this.toggleSubTaskCheckboxes(true);
        } else {
            document.querySelectorAll('.editField').forEach(elem => {
                elem.disabled = true;
            });
            this.ouChoicesEditSelect.disable();
            this.ouDepartmentChoicesEditSelect.disable();
            this.userChoicesEditSelect.disable();
            document.getElementById('saveEditTaskBtn').hidden = true;
            document.getElementById('editTaskBtn').hidden = false;
            performButton.hidden = false;
            this.nameField.disabled = true
            document.getElementById("linksViewContainer").hidden = false;
            document.getElementById("subTaskViewContainer").hidden = false;
            document.getElementById("linksEditContainer").hidden = true;
            document.getElementById("subTaskEditContainer").hidden = true;
            document.getElementById("addLinkBtn").hidden = true;
            document.getElementById("subTaskAddLinkBtn").hidden = true;
            this.notificationSelectHandler.disable();
            this.toggleSubTaskCheckboxes(false);
        }
    }

    this.toggleSubTaskCheckboxes = function(enabled) {
        const checkboxes = document.querySelectorAll('#subTaskEditContainer input[type="checkbox"]');

        checkboxes.forEach(checkbox => {
            checkbox.disabled = !enabled;
        });
    }

    this.loadViewAndEditForm = function() {
        const self = this;
        this.userChoicesEditSelect = choiceService.initUserSelect('userSelect');
        this.ouChoicesEditSelect = choiceService.initOUSelect('ouSelect');
        this.ouDepartmentChoicesEditSelect = choiceService.initOUSelect('departmentOuSelect');
        const currentValue = this.ouDepartmentChoicesEditSelect.getValue(true);

        if (!currentValue || currentValue === '' || currentValue === null) {
            this.ouDepartmentChoicesEditSelect.setChoices([{ value: '', label: 'Vælg forvaltning...', selected: true }], 'value', 'label', false);
        }
        this.nameField = document.getElementById("taskNameField")
        this.userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.userChoicesEditSelect);
        });

        document.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = true;
        });
        this.ouChoicesEditSelect.disable();
        this.ouDepartmentChoicesEditSelect.disable();
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