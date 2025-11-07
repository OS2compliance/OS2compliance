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

        initFormValidationForForm('editForm');
        if (taskType === 'CHECK') {
            initFormValidationForForm('completeTaskForm', () => {
                const comment = document.getElementById("completionComment");
                const taskResultSelect = document.getElementById("taskResultSelect");
                const taskType = document.getElementById("taskType");

                if (taskResultSelect.value === 'NO_ERROR') {
                    comment.classList.remove('is-invalid');
                    return true;
                } else if (taskResultSelect.value !== 'NO_ERROR' && comment.value.trim()) {
                    comment.classList.remove('is-invalid');
                    return true;
                } else {
                    comment.classList.add('is-invalid');
                    return false;
                }
            });
        }
        else {
            initFormValidationForForm('completeTaskForm', () => {
                const comment = document.getElementById("completionComment");
                if (!comment.value) {
                    comment.classList.add('is-invalid');
                    return false;
                }
                else {
                    comment.classList.remove('is-invalid');
                    return true;
                }
            })
        }

        initDatepicker("#deadlineBtn", "#deadline");
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

    this.fitDescription = function (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = textarea.scrollHeight + 'px';
    }

    this.loadDescriptionTemplateSelect = function() {
        const select = document.getElementById('taskDescriptionTemplateSelect');
        const descriptionField = document.getElementById('description');
        let previousDescription = ''; // Store previous value

        select.addEventListener("change", async function () {
            const selectedValue = this.value;

            // If "Ingen valgt" (no selection) or empty value
            if (!selectedValue || selectedValue === '') {
                descriptionField.value = previousDescription;
                descriptionField.disabled = false;
                return;
            }

            // Save current description before replacing it
            if (descriptionField.value) {
                previousDescription = descriptionField.value;
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
            document.getElementById("linksViewContainer").hidden = true;
            document.getElementById("linksEditContainer").hidden = false;
            document.getElementById("addLinkBtn").hidden = false;
            this.notificationSelectHandler.enable();
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
            document.getElementById("linksEditContainer").hidden = true;
            document.getElementById("addLinkBtn").hidden = true;
            this.notificationSelectHandler.disable();
        }
    }

    this.loadViewAndEditForm = function() {
        const self = this;
        this.userChoicesEditSelect = choiceService.initUserSelect('userSelect');
        this.ouChoicesEditSelect = choiceService.initOUSelect('ouSelect');
        this.ouDepartmentChoicesEditSelect = choiceService.initOUSelect('departmentOuSelect');
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