// This happens on page load
const createRiskService = new CreateRiskService();
const preselect = new Preselect();

document.addEventListener("DOMContentLoaded", function(event) {
    createRiskService.init();
    preselect.init();

    // Preselect the "behandlingsaktivitet"
    let activity = document.getElementById("registerSelect");
    const currentRegisterName = document.getElementById("breadcrump-register-name");
    const regiName = currentRegisterName.getAttribute("data-register-name");

    function tryPreselect() {
        for (let option of activity.options) {
            console.log(option.value);
            if (option.value === regiName) {
                activity.value = option.value;
                activity.dispatchEvent(new Event("change"));
                break;
            }
        }
    }
    tryPreselect();
});

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}

function Preselect() {
    this.init = function () {
        // Preselect the type
        let type = document.getElementById("threatAssessmentType");
        type.value = "REGISTER";
        type.dispatchEvent(new Event("change"));
    }
}

function CreateRiskService() {

    this.init = function () {
        let self = this;
        this.modalContainer = document.getElementById('createModal');

        const registerSelect = this.getScopedElementById('registerSelect');
        const assetSelect = this.getScopedElementById('assetSelect');
        this.registerChoicesSelect = initRegisterSelect(registerSelect);
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        this.userChoicesSelect = choiceService.initUserSelect("createRiskUserSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("createRiskOuSelect");

        this.userChoicesSelect.passedElement.element.addEventListener('change', function () {
            const userUuid = self.userChoicesSelect.passedElement.element.value;
            self.userChanged(userUuid);
        });

        this.typeChanged(this.getScopedElementById("threatAssessmentType").value);
        this.getScopedElementById('threatAssessmentType').addEventListener('change', function () {
            self.typeChanged(this.value);
        });

        this.assetChoicesSelect.passedElement.element.addEventListener('change', function () {
            self.clearAssetValidationError();
            self.loadAssetSection();
        });
        let selectedRegisterElement = this.getScopedElementById("registerSelect");
        this.registerChoicesSelect.passedElement.element.addEventListener('change', function () {
            self.clearRegisterValidationError();
            loadRegisterResponsible(selectedRegisterElement, self.userChoicesSelect);
        });

        this.getScopedElementById('sendEmailcheckbox').addEventListener('change', function () {
            self.sendEmailChanged(this.checked);
        });

        const presentSelect = this.getScopedElementById('presentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('presentAtMeetingSelect');
        }

        initFormValidationForForm("createRiskModal",
            () => {
                return this.validateEntitySelection() &&
                    this.validateChoicesAndCheckboxesRisk(this.userChoicesSelect, this.ouChoicesSelect);
            });

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
        this.selectedType = selectedType;
    }

    this.userChanged = function (userUuid) {
        fetch(`/rest/ous/user/` + userUuid)
            .then(response => response.json()
                .then(data => {
                    this.ouChoicesSelect.setChoices([data], 'uuid', 'name');
                    this.ouChoicesSelect.setChoiceByValue(data.uuid);
                })).catch(error => toastService.error(error));
    }

    this.clearRegisterValidationError = function () {
        this.getScopedElementById("registerSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("registerError").classList.remove('show');
    }

    this.clearAssetValidationError = function () {
        this.getScopedElementById("assetSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("assetError").classList.remove('show');
    }

    this.validateEntitySelection = function () {
        let result = true;
        if (this.selectedType === "ASSET") {
            // Check that at least one asset is selected
            let assetSelect = this.getScopedElementById("assetSelect");
            let assetSelected = assetSelect.value !== "";
            if (assetSelected) {
                this.clearAssetValidationError();
            } else {
                assetSelect.parentElement.classList.add('is-invalid');
                this.getScopedElementById("assetError").classList.add('show');
            }
            result &= assetSelected;
        } else if (this.selectedType === 'REGISTER') {
            let registerSelect = this.getScopedElementById("registerSelect");
            let registerSelected = registerSelect.value !== "";
            if (registerSelected) {
                this.clearRegisterValidationError();
            } else {
                registerSelect.parentElement.classList.add('is-invalid');
                this.getScopedElementById("registerError").classList.add('show');
            }
            result &= registerSelected;
        }
        return result;
    };

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
        fetch(`/rest/risks/asset?assetIds=${selectedAsset}`)
            .then(response => response.json()
                .then(data => {
                    let user = data.users?.users[0];
                    if (user != null) {
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

    this.getScopedElementById = function (id) {
        return this.modalContainer.querySelector(`#${id}`);
    }
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

function initAssetSelectRisk(assetSelectElement) {
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