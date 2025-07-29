const createSectionService = new CreateSectionService();

document.addEventListener("shown.bs.modal", function(event) {
    createSectionService.toggleTextAreas();
});

function CreateSectionService() {
    this.sectionModalDialog = null;
    this.headerModalDialog = null;

    this.openRequirementModal = function(element) {
        const id = element.dataset.id;
        fetch(`/standards/section/form/` + id)
            .then(response => response.text()
                .then(data => {
                    this.sectionModalDialog = document.getElementById('sectionFormDialog');
                    this.sectionModalDialog.innerHTML = data;
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

                    const createSectionModal = new bootstrap.Modal(this.sectionModalDialog);
                    createSectionModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openHeaderModal = function(element) {
        const id = element.dataset.id;

        fetch(`/standards/section/header/form/` + id)
            .then(response => response.text()
                .then(data => {
                    this.headerModalDialog = document.getElementById('headerFormDialog');
                    this.headerModalDialog.innerHTML = data;
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

                    const headerModal = new bootstrap.Modal(this.headerModalDialog);
                    headerModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.toggleNSIS = function () {
        const checkbox = document.getElementById('toggleNSIS');
        if (checkbox.checked) {
            // Toggle the two text fields
            document.getElementById('nsisSmart').style.display = '';
            document.getElementById('nsisPractice').style.display = '';
            document.getElementById('description').style.display = 'none';
        }
        else {
            // Toggle the single text field
            document.getElementById('nsisSmart').style.display = 'none';
            document.getElementById('nsisPractice').style.display = 'none';
            document.getElementById('description').style.display = '';
        }
    }

    this.toggleTextAreas = function () {
        let editors = document.querySelectorAll(`.description`);
        for (let i= 0; i< editors.length; ++i) {
            const elem = editors[i];
            window.CreateCkEditor(elem, editor => {});
        }
    }

}
