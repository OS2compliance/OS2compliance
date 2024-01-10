

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function updateTypeSelect(choices, search, types) {
    fetch( `/rest/relatable/autocomplete?types=${types}&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}

function initRegisterSelect(registerSelectElement) {
    const registerChoices = initSelect(registerSelectElement);
    updateTypeSelect(registerChoices, "", "REGISTER");
    registerSelectElement.addEventListener("search",
        function(event) {
            updateTypeSelect(registerChoices, event.detail.value, "REGISTER");
        },
        false,
    );
    return registerChoices;
}

function initAssetSelect(assetSelectElement) {
    const assetChoices = initSelect(assetSelectElement);
    updateTypeSelect(assetChoices, "", "ASSET");
    assetSelectElement.addEventListener("search",
        function(event) {
            updateTypeSelect(assetChoices, event.detail.value, "ASSET");
        },
        false,
    );
    return assetChoices;
}


function loadRegisterResponsible(selectedRegisterElement, userChoicesSelect) {
    let selectedRegister = selectedRegisterElement.value;
    fetch( `/rest/risks/register?registerId=${selectedRegister}`)
        .then(response => response.json()
            .then(user => {
                if (user.uuid != null) {
                    userChoicesSelect.setChoiceByValue(user.uuid);
                } else {
                    userChoicesSelect.removeActiveItems();
                }

                if (user.elementName != null) {
                    document.getElementById('name').value = user.elementName;
                } else {
                    document.getElementById('name').value = "";
                }
            }))
        .catch(error => toastService.error(error));
}

const createRiskService = new CreateRiskService();
const copyRiskService = new CopyRiskService();
document.addEventListener("DOMContentLoaded", function(event) {
    createRiskService.init();
});

function CopyRiskService() {
    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showCopyDialog = function(threatAssessmentId) {
        let container = document.getElementById('copyAssessmentContainer');
        fetch(`${baseUrl}${threatAssessmentId}/copy`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function() {
        this.modalContainer = document.getElementById('copyModal');
        const registerSelect = this.getScopedElementById('registerSelect');
        if (registerSelect !== null) {
            this.registerChoicesSelect = initRegisterSelect(registerSelect);
        }
        const assetSelect = this.getScopedElementById('assetSelect');
        if (assetSelect !== null) {
            this.assetChoicesSelect = initAssetSelect(assetSelect);
        }
        this.userChoicesSelect = initUserSelect("userSelect");
        this.ouChoicesSelect = initOUSelect("ouSelect");
        initFormValidationForForm("copyRiskModalForm",
            () => this.validate());

        this.copyAssessmentModal = new bootstrap.Modal(this.modalContainer);
        this.copyAssessmentModal.show();
    }

    this.validate = function() {
        let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        if (this.assetChoicesSelect != null) {
            result &= checkInputField(this.assetChoicesSelect, true);
        } else if (this.registerChoicesSelect != null) {
            result &= validateChoices(this.registerChoicesSelect);
        }
        return result;
    }
}

function CreateRiskService() {

    this.init = function () {
        let self = this;
        this.modalContainer = document.getElementById('createModal');

        const registerSelect = this.getScopedElementById('registerSelect');
        const assetSelect = this.getScopedElementById('assetSelect');
        this.registerChoicesSelect = initRegisterSelect(registerSelect);
        this.assetChoicesSelect = initAssetSelect(assetSelect);
        this.userChoicesSelect = initUserSelect("userSelect");
        this.ouChoicesSelect = initOUSelect("ouSelect");

        this.typeChanged(this.getScopedElementById("threatAssessmentType").value);
        this.getScopedElementById('threatAssessmentType').addEventListener('change', function() {
            self.typeChanged(this.value);
        });

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function() {
            self.loadAssetSection();
        });
        let selectedRegisterElement = this.getScopedElementById("registerSelect");
        this.registerChoicesSelect.passedElement.element.addEventListener('change', function() {
            loadRegisterResponsible(selectedRegisterElement, self.userChoicesSelect);
        });

        this.getScopedElementById('sendEmailcheckbox').addEventListener('change', function() {
            self.sendEmailChanged(this.checked);
        });

        initFormValidationForForm("createRiskModal",
            () => this.validateChoicesAndCheckboxesRisk(this.userChoicesSelect, this.ouChoicesSelect));
    }

    this.typeChanged = function (selectedType) {
        if (selectedType === 'ASSET') {
            this.getScopedElementById("registerSelectRow").style.display = 'none';
            this.getScopedElementById("assetSelectRow").style.display = '';
        } else if (selectedType === 'REGISTER') {
            this.getScopedElementById("registerSelectRow").style.display = '';
            this.getScopedElementById("assetSelectRow").style.display = 'none';
        } else {
            this.getScopedElementById("registerSelectRow").style.display = 'none';
            this.getScopedElementById("assetSelectRow").style.display = 'none';
        }
        this.getScopedElementById("inheritRow").style.display = 'none';
        this.registerChoicesSelect.removeActiveItems();
        this.assetChoicesSelect.removeActiveItems();
    }

    this.validateChoicesAndCheckboxesRisk = function (...choiceList) {
        let result = true;
        for (let i = 0; i < choiceList.length; i++) {
            if (!checkInputField(choiceList[i])) {
                result = false;
            }
        }
        let registered = this.getScopedElementById("registered");
        let organisation = this.getScopedElementById("organisation");
        if (!registered.checked && !organisation.checked) {
            registered.classList.add('is-invalid');
            organisation.classList.add('is-invalid');
            this.getScopedElementById("checkboxError").classList.add('show');
            result = false;
        } else {
            registered.classList.remove('is-invalid');
            organisation.classList.remove('is-invalid');
            this.getScopedElementById("checkboxError").classList.remove('show');
        }

        return result;
    }

    this.loadAssetSection = function () {
        const selectedAsset = this.getScopedElementById("assetSelect").value;
        fetch( `/rest/risks/asset?assetIds=${selectedAsset}`)
            .then(response => response.json()
                .then(data => {
                    let user = data.user;
                    if (user.uuid != null) {
                        this.userChoicesSelect.setChoiceByValue(user.uuid);
                    } else {
                        this.userChoicesSelect.removeActiveItems();
                    }

                    if (data.elementName != null) {
                        this.getScopedElementById('name').value = data.elementName;
                    } else {
                        this.getScopedElementById('name').value = "";
                    }

                    // set text in table
                    this.getScopedElementById("RF").innerHTML = data.rf === 0 ? "" : data.rf;
                    this.getScopedElementById("OF").innerHTML = data.of === 0 ? "" : data.of;
                    this.getScopedElementById("RI").innerHTML = data.ri === 0 ? "" : data.ri;
                    this.getScopedElementById("OI").innerHTML = data.oi === 0 ? "" : data.oi;
                    this.getScopedElementById("RT").innerHTML = data.rt === 0 ? "" : data.rt;
                    this.getScopedElementById("OT").innerHTML = data.ot === 0 ? "" : data.ot;

                    this.getScopedElementById("inheritRow").style.display = '';
                }))
            .catch(error => toastService.error(error));
    }

    this.sendEmailChanged = function (checked) {
        this.getScopedElementById('sendEmail').value = checked;
    }

    this.getScopedElementById = function(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }


}
