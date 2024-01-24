
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

        this.createTaskUserChoicesEditSelect = initUserSelect('taskUserSelect');
        this.createTaskOuChoicesEditSelect = initOUSelect('taskOuSelect');
        this.createTaskUserChoicesEditSelect.passedElement.element.addEventListener('change', function() {
            checkInputField(self.createTaskUserChoicesEditSelect);
        });
        initFormValidationForForm('taskCreateForm',
            () => validateChoices(
                this.createTaskUserChoicesEditSelect, this.createTaskOuChoicesEditSelect));
    }

    this.show = function(elem = null) {
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

                    // if elem != null it means that the method is called from the risk view page
                    if (elem != null) {
                        var riskId = elem.dataset.riskid;
                        var customId = elem.dataset.customid;
                        var catalogIdentifier = elem.dataset.catalogidentifier;
                        modal.querySelector('#threatAssessmentExplainer').style.display = '';
                        modal.querySelector('#taskRiskId').value = riskId;
                        modal.querySelector('#riskCustomId').value = customId;
                        modal.querySelector('#riskCatalogIdentifier').value = catalogIdentifier;
                    } else {
                        modal.querySelector('#threatAssessmentExplainer').style.display = 'none';
                        modal.querySelector('#taskRiskId').value = null;
                        modal.querySelector('#riskCustomId').value = null;
                        modal.querySelector('#riskCatalogIdentifier').value = null;
                    }

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
