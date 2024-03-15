
const createTaskService = new CreateTaskService();

function CreateTaskService() {
    this.taskModalDialog = null;
    this.createTaskOuChoicesEditSelect = null;

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
         this.createTaskOuChoicesEditSelect = initOUSelect('taskCreateFormTaskOuSelect');

        this.createTaskUserChoicesEditSelect = initUserSelect('taskCreateFormTaskUserSelect');
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
