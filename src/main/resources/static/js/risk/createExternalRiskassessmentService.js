class CreateExternalRiskassessmentService {
    assetChoicesSelect
    formElement
    createResponsibleUserChoice = null
    createResponsibleOuChoice = null
    editResponsibleUserChoice = null
    editResponsibleOUChoice = null

    constructor () {
    }

    initCreateModal() {
        const assetSelect = document.getElementById('externalCreateRiskassessmentAssetSelect');
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        this.createResponsibleOuChoice = this.#initSearchOus('externalCreateOuSelect')
        this.createResponsibleUserChoice = this.#initSearchUsers('externalCreateUserSelect')


        const registerSelect = document.getElementById('createRegisterSelect');

        this.registerChoicesSelect = initRegisterSelect(registerSelect);

        const self = this
        const modalContainerElement = document.getElementById("createExternalRiskassessmentModal")

        const typeInput = document.getElementById("createThreatAssessmentType")
        this.typeChanged(typeInput.value, modalContainerElement);
        typeInput.addEventListener('change', function(event) {
            const modalContainerElement = document.getElementById("createExternalRiskassessmentModal")
            self.typeChanged(this.value, modalContainerElement);
        });

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function() {
            const modalContainerElement = document.getElementById("createExternalRiskassessmentModal")
            self.clearAssetValidationError(modalContainerElement, 'externalCreateRiskassessmentAssetSelect');
        });

    }

    initEditModal() {
        const assetSelect = document.getElementById('externalEditRiskassessmentAssetSelect');
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        this.editResponsibleOUChoice = this.#initSearchOus('externalEditOuSelect')
        this.editResponsibleUserChoice = this.#initSearchUsers('externalEditUserSelect')

        const self = this

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function() {
            const modalContainerElement = document.getElementById("createExternalRiskassessmentModal")
            self.clearAssetValidationError(modalContainerElement, 'externalEditRiskassessmentAssetSelect');
            // self.loadAssetSection(modalContainerElement, 'externalEditRiskassessmentAssetSelect');
        });
    }

    formReset(id) {
        this.formElement = document.getElementById(id);
        this.formElement.reset();
    }

    collectCreateData() {
        const assetSelect = document.getElementById('externalCreateRiskassessmentAssetSelect');
        const linkInput = document.getElementById('linkInput');
        const typeElement = document.getElementById('createThreatAssessmentType')
        const registerSelect = document.getElementById('registerSelect')
        const nameElement = document.getElementById('createName')

        const responsibleUserUuid = this.createResponsibleUserChoice.getValue(true)
        const responsibleOuUuid = this.createResponsibleOuChoice.getValue(true)

        return {
            type: typeElement.value,
            riskId: null,
            assetIds : assetSelect ? [...assetSelect.selectedOptions].map(option => option.value) : [],
            registerId : registerSelect ? registerSelect.value : null,
            link: linkInput.value ? linkInput.value : "",
            name : nameElement.value,
            responsibleUserUuid: responsibleUserUuid,
            responsibleOuUuid: responsibleOuUuid,
        }
    }

    collectEditData(riskId) {
        const assetSelect = document.getElementById('externalEditRiskassessmentAssetSelect');
        const linkInput = document.getElementById('linkInput');
        const typeElement = document.getElementById('editThreatAssessmentType')
        const nameElement = document.getElementById('editName')

        const responsibleUserUuid = this.editResponsibleUserChoice.getValue(true)
        const responsibleOuUuid = this.editResponsibleOUChoice.getValue(true)

        return {
            type: typeElement.value,
            riskId: riskId ? riskId : null,
            assetIds : assetSelect ? [...assetSelect.selectedOptions].map(option => option.value) : [],
            registerId : registerSelect ? registerSelect.value : null,
            link: linkInput.value ? linkInput.value : "",
            name : nameElement.value,
            responsibleUserUuid: responsibleUserUuid,
            responsibleOuUuid: responsibleOuUuid,
        }
    }

    async submitNewExternal(riskId) {
        const data = riskId ? this.collectEditData(riskId) : this.collectCreateData()

        const url = `${restUrl}/external/create`
        const response = await fetch(url, {
            method: "POST",
            headers: {
                'X-CSRF-TOKEN': token,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
        }

        location.reload()
    }

    async editExternalClicked(riskId) {
        const url = `${baseUrl}external/${riskId}/edit`
        const response = await fetch(url, {
            method: "GET",
            headers: {
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
            throw Error ("could not load external riskassessment edit view: " + riskId + "")
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById("external_modal_container")
        externalModalContainer.innerHTML = responseText

        this.initEditModal()

        const modalElement = externalModalContainer.querySelector('#createExternalRiskassessmentModal')
        const modal = new bootstrap.Modal(modalElement);
        modal.show();

    }

    async createExternalClicked() {
        const url = `${baseUrl}external/create`
        const response = await fetch(url, {
            method: "GET",
            headers: {
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok)  {
            toastService.error(response.statusText)
            throw Error ("could not load external riskassessment create view: " + riskId + "")
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById("external_modal_container")
        externalModalContainer.innerHTML = responseText

        this.initCreateModal()

        const modalElement = externalModalContainer.querySelector('#createExternalRiskassessmentModal')
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

    typeChanged (selectedType, parentElement) {
        if (selectedType === 'ASSET') {
            parentElement.querySelector("#registerSelectRow").style.display = 'none';
            parentElement.querySelector("#assetSelectRow").style.display = '';
        } else if (selectedType === 'REGISTER') {
            parentElement.querySelector("#registerSelectRow").style.display = '';
            parentElement.querySelector("#assetSelectRow").style.display = 'none';
        } else {
            parentElement.querySelector("#registerSelectRow").style.display = 'none';
            parentElement.querySelector("#assetSelectRow").style.display = 'none';
        }
        const inheritRow = parentElement.querySelector("#inheritRow")
        if (inheritRow) {
            inheritRow.style.display = '';
        }
        this.registerChoicesSelect.removeActiveItems();
        this.assetChoicesSelect.removeActiveItems();
        this.selectedType = selectedType;
    }

    clearAssetValidationError(parent, assetSelectId) {
        parent.querySelector(`#${assetSelectId}`).parentElement.classList.remove('is-invalid');
        parent.querySelector("#assetError").classList.remove('show');
    }


    #initSearchOus(elementId){
       return choiceService.initOUSelect(elementId)
    }

    #initSearchUsers(elementId){
        return choiceService.initUserSelect(elementId)
    }
}
