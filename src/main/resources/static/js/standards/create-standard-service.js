const createStandardService = new CreateStandardService();

function CreateStandardService() {
    this.standardModalDialog = null;

    this.openStandardCreateModal = function() {
        fetch(`/standards/form`)
            .then(response => response.text()
                .then(data => {
                    this.standardModalDialog = document.getElementById('standardFormDialog');
                    this.standardModalDialog.innerHTML = data;
                    // TODO: (Edit) create task modal - explainer and riskId
                    // if elem != null it means that the method is called from the risk view page
                    // if (elem != null) {
                    //     var riskId = elem.dataset.riskid;
                    //     var customId = elem.dataset.customid;
                    //     var catalogIdentifier = elem.dataset.catalogidentifier;
                    //     this.taskModalDialog.querySelector('#taskCreateFormThreatAssessmentExplainer').style.display = '';
                    //     this.taskModalDialog.querySelector('#taskCreateFormTaskRiskId').value = riskId;
                    //     this.taskModalDialog.querySelector('#taskCreateFormRiskCustomId').value = customId;
                    //     this.taskModalDialog.querySelector('#taskCreateFormRiskCatalogIdentifier').value = catalogIdentifier;
                    // } else {
                    //     this.taskModalDialog.querySelector('#taskCreateFormThreatAssessmentExplainer').style.display = 'none';
                    //     this.taskModalDialog.querySelector('#taskCreateFormTaskRiskId').value = null;
                    //     this.taskModalDialog.querySelector('#taskCreateFormRiskCustomId').value = null;
                    //     this.taskModalDialog.querySelector('#taskCreateFormRiskCatalogIdentifier').value = null;
                    // }

                    const createTaskModal = new bootstrap.Modal(this.standardModalDialog);
                    createTaskModal.show();
                }))
            .catch(error => toastService.error(error));
    }
}
