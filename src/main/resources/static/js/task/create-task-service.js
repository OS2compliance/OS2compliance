
const createTaskService = new CreateTaskService();

function CreateTaskService() {

    this.selectCreateTaskOption = function(value) {
        const form = document.querySelector('#taskCreateForm');
        const repetitionField = form.querySelector('#repetition');
        if (value === 'TASK') {
            repetitionField.value = 'NONE';
        }
        repetitionField.disabled = value !== 'CHECK';
    }

    this.loaded = function() {
        let self = this;
        this.selectCreateTaskOption('TASK');
        initDatepicker("#taskDeadlineBtn", "#taskDeadline");
         this.createTaskOuChoicesEditSelect = initOUSelect('taskOuSelect');

        this.createTaskUserChoicesEditSelect = initUserSelect('taskUserSelect');
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

    this.show = function(riskId = null) {
        this.loading = true;
        fetch(`/tasks/form`)
            .then(response => response.text()
                .then(data => {
                    document.getElementById('taskFormDialog').innerHTML = data;
                    this.loaded();
                    this.initTaskRelationSelect();
                    initTagSelect('createTaskTagsSelect');
                    // create task modal - explainer and riskId
                    var modal = document.querySelector('#taskFormDialog');
                    modal.querySelector('#threatAssessmentExplainer').style.display = '';
                    modal.querySelector('#taskRiskId').value = riskId;

                    const createTaskModal = new bootstrap.Modal(modal);
                    createTaskModal.show();
                }))
            .catch(error => toastService.error(error))
            .finally(() => this.loading = false);
    }

    this.initTaskRelationSelect = function() {
        const relationsSelect = document.getElementById('createTaskRelationsSelect');
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
