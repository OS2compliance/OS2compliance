

class CreateExternalRiskassessmentService {
    assetChoicesSelect
    formElement

    constructor () {
//        this.init()
    }

    init() {
        const assetSelect = document.getElementById('externalRiskassessmentAssetSelect');
        // this.assetChoicesSelect = this.#initAssetSelect(assetSelect);
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);


        const registerSelect = document.getElementById('registerSelect');

        this.registerChoicesSelect = initRegisterSelect(registerSelect);

        const self = this

        const typeInput = document.getElementById("threatAssessmentType")
        this.typeChanged(typeInput.value);
        typeInput.addEventListener('change', function(event) {
            self.typeChanged(this.value);
        });

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function() {
            self.clearAssetValidationError();
            self.loadAssetSection();
        });

    }

    formReset() {
        this.formElement = document.getElementById('createExternalRiskassessmentForm');
        this.formElement.reset();
    }

    async submitNewExternal(riskId) {
        const assetSelect = document.getElementById('externalRiskassessmentAssetSelect');
        const linkInput = document.getElementById('linkInput');
        const typeElement = document.getElementById('threatAssessmentType')
        const registerSelect = document.getElementById('registerSelect')
        const nameElement = document.getElementById('name')
        const data = {
            type: typeElement.value,
            riskId: riskId ? riskId : null,
            assetId : assetSelect ? [...assetSelect.selectedOptions].map(option => option.value) : [],
            registerId : registerSelect ? registerSelect.value : null,
            link: linkInput.value ? linkInput.value : "",
            name : nameElement.value
        }

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
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById("external_modal_container")
        externalModalContainer.innerHTML = responseText

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
        }

        const responseText = await response.text()

        const externalModalContainer = document.getElementById("external_modal_container")
        externalModalContainer.innerHTML = responseText

        this.init()

        const modalElement = externalModalContainer.querySelector('#createExternalRiskassessmentModal')
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }

    typeChanged (selectedType) {
        if (selectedType === 'ASSET') {
            document.getElementById("registerSelectRow").style.display = 'none';
            document.getElementById("assetSelectRow").style.display = '';
        } else if (selectedType === 'REGISTER') {
            document.getElementById("registerSelectRow").style.display = '';
            document.getElementById("assetSelectRow").style.display = 'none';
        } else {
            document.getElementById("registerSelectRow").style.display = 'none';
            document.getElementById("assetSelectRow").style.display = 'none';
        }
        document.getElementById("inheritRow").style.display = 'none';
        this.registerChoicesSelect.removeActiveItems();
        this.assetChoicesSelect.removeActiveItems();
        this.selectedType = selectedType;
    }

    clearAssetValidationError() {
        document.getElementById("assetSelect").parentElement.classList.remove('is-invalid');
        document.getElementById("assetError").classList.remove('show');
    }


    loadAssetSection () {
        const selectedAsset = document.getElementById("assetSelect").value;
        fetch( `/rest/risks/asset?assetIds=${selectedAsset}`)
        .then(response => response.json()
        .then(data => {
            if (data.elementName != null) {
                document.getElementById('name').value = data.elementName;
            } else {
                document.getElementById('name').value = "";
            }

            // set text in table
            document.getElementById("RF").innerHTML = data.rf === 0 ? "" : data.rf;
            document.getElementById("OF").innerHTML = data.of === 0 ? "" : data.of;
            document.getElementById("RI").innerHTML = data.ri === 0 ? "" : data.ri;
            document.getElementById("OI").innerHTML = data.oi === 0 ? "" : data.oi;
            document.getElementById("RT").innerHTML = data.rt === 0 ? "" : data.rt;
            document.getElementById("OT").innerHTML = data.ot === 0 ? "" : data.ot;

            document.getElementById("inheritRow").style.display = '';
        }))
        .catch(error => toastService.error(error));
    }
}
