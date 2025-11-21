export class CreateThreatAssessmentService {

    init() {
        this.modalContainer = document.getElementById('createModal');

        this.initRegisterSelect()
        this.initAssetSelect()
        this.initUserSelect()
        this.initOuSelect()

        this.initTypeSelect()
        this.initRegisterResponsible()

        this.initSendEmail()

        this.initPresentSelect()

        const catalogSelect = this.getScopedElementById('threatCatalogSelect');
        initSelect(catalogSelect);

        this.initSocietyCheck()

        initFormValidationForForm("createRiskModal",
            () => {
                return this.validateEntitySelection() &&
                    this.validateChoicesAndCheckboxesRisk(this.userChoicesSelect, this.ouChoicesSelect) && validateInputFieldLength("name", 255);
            });

        globalThis.formReset = formReset;
    }

    initTypeSelect() {
        this.typeChanged(this.getScopedElementById("threatAssessmentType").value);
        const element = this.getScopedElementById('threatAssessmentType')
        element.addEventListener('change', () => {
            this.typeChanged(element.value);
        });
    }

    initSocietyCheck() {
        let societyCheckbox = this.getScopedElementById("society");
        let authenticityCheckbox = this.getScopedElementById("authenticity");
        let authenticitySection = this.getScopedElementById("authenticitySection");
        societyCheckbox.addEventListener('change', () => {
            if (societyCheckbox.checked) {
                // show authenticity checkbox
                authenticitySection.hidden = false;
            } else {
                // hide authenticity checkbox and reset
                authenticitySection.hidden = true;
                authenticityCheckbox.checked = false;
            }
        });
    }

    initRegisterSelect() {
        const registerSelect = this.getScopedElementById('registerSelect');
        this.registerChoicesSelect = initRegisterSelect(registerSelect);
    }

    initAssetSelect() {
        const assetSelect = this.getScopedElementById('assetSelect');
        this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        this.assetChoicesSelect.passedElement.element.addEventListener('change', () => {
            this.clearAssetValidationError();
            this.loadAssetSection();
        });
    }

    initUserSelect() {
        this.userChoicesSelect = choiceService.initUserSelect("createRiskUserSelect");
        this.userChoicesSelect.passedElement.element.addEventListener('change', () => {
            const userUuid = this.userChoicesSelect.passedElement.element.value;
            userChanged(userUuid);
        });
    }

    initOuSelect() {
        this.ouChoicesSelect = choiceService.initOUSelect("createRiskOuSelect");
    }

    initSendEmail() {
        this.getScopedElementById('sendEmailcheckbox').addEventListener('change', () => {
            this.sendEmailChanged(this.checked);
        });
    }

    initRegisterResponsible() {
        let selectedRegisterElement = this.getScopedElementById("registerSelect");
        this.registerChoicesSelect.passedElement.element.addEventListener('change', () => {
            this.clearRegisterValidationError();
            loadRegisterResponsible(selectedRegisterElement, this.userChoicesSelect);
        });
    }

    initPresentSelect() {
        const presentSelect = this.getScopedElementById('presentAtMeetingSelect');
        if (presentSelect !== null) {
            choiceService.initUserSelect('presentAtMeetingSelect');
        }
    }

    typeChanged(selectedType) {
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


    clearRegisterValidationError() {
        this.getScopedElementById("registerSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("registerError").classList.remove('show');
    }

    clearAssetValidationError() {
        this.getScopedElementById("assetSelect").parentElement.classList.remove('is-invalid');
        this.getScopedElementById("assetError").classList.remove('show');
    }

    validateEntitySelection() {
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

    validateChoicesAndCheckboxesRisk(...choiceList) {
        let result = true;
        for (const element of choiceList) {
            if (!checkInputField(element)) {
                result = false;
            }
        }
        let registered = this.getScopedElementById("registered");
        let organisation = this.getScopedElementById("organisation");
        let society = this.getScopedElementById("society");
        if (!registered.checked && !organisation.checked && !society.checked) {
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

    loadAssetSection() {
        const selectedAsset = this.getScopedElementById("assetSelect").value;
        fetch(`/rest/risks/asset?assetIds=${selectedAsset}`)
            .then(response => response.json()
                .then(data => {
                    let user = data.users?.users[0];
                    if (user) {
                        this.userChoicesSelect.setChoiceByValue(user.uuid);
                    } else {
                        this.userChoicesSelect.removeActiveItems();
                    }

                    if (data.elementName) {
                        this.getScopedElementById('name').value = data.elementName;
                    } else {
                        this.getScopedElementById('name').value = "";
                    }

                    // set text in table
                    this.getScopedElementById("RF").innerHTML = data.rf === 0 ? "" : data.rf;
                    this.getScopedElementById("OF").innerHTML = data.of === 0 ? "" : data.of;
                    this.getScopedElementById("SF").innerHTML = data.sf === 0 ? "" : data.sf;
                    this.getScopedElementById("RI").innerHTML = data.ri === 0 ? "" : data.ri;
                    this.getScopedElementById("OI").innerHTML = data.oi === 0 ? "" : data.oi;
                    this.getScopedElementById("SI").innerHTML = data.si === 0 ? "" : data.si;
                    this.getScopedElementById("RT").innerHTML = data.rt === 0 ? "" : data.rt;
                    this.getScopedElementById("OT").innerHTML = data.ot === 0 ? "" : data.ot;
                    this.getScopedElementById("ST").innerHTML = data.st === 0 ? "" : data.st;
                    this.getScopedElementById("SA").innerHTML = data.sa === 0 ? "" : data.sa;

                    this.getScopedElementById("inheritRow").style.display = '';
                }))
            .catch(error => toastService.error(error));
    }

    sendEmailChanged(checked) {
        this.getScopedElementById('sendEmail').value = checked;
    }

    getScopedElementById(id) {
        return this.modalContainer.querySelector(`#${id}`);
    }
}

export function userChanged(userUuid) {
    fetch(`/rest/ous/user/` + userUuid)
        .then(response => {
            if (response.status === 204) {
                return;
            }

            if (response.ok) {
                response.json().then(data => {
                    this.ouChoicesSelect.setChoices([data], 'uuid', 'name');
                    this.ouChoicesSelect.setChoiceByValue(data.uuid);
                });
            }
        }).catch(error => toastService.error(error));
}

export function initRegisterSelect(registerSelectElement) {
    const registerChoices = initSelect(registerSelectElement);
    registerSelectElement.addEventListener("search",
        function (event) {
            updateTypeSelect(registerChoices, event.detail.value, "REGISTER");
        },
        false,
    );
    return registerChoices;
}

export function initAssetSelectRisk(assetSelectElement) {
    const assetChoices = initSelect(assetSelectElement);
    updateTypeSelect(assetChoices, "", "ASSET");
    assetSelectElement.addEventListener("search",
        function (event) {
            updateTypeSelect(assetChoices, event.detail.value, "ASSET");
        },
        false,
    );
    return assetChoices;
}

function loadRegisterResponsible(selectedRegisterElement, userChoicesSelect) {
    let selectedRegister = selectedRegisterElement.value;
    fetch(`/rest/risks/register?registerId=${selectedRegister}`)
        .then(response => response.json()
            .then(data => {
                let user = data.users[0];
                if (user) {
                    userChoicesSelect.setChoiceByValue(user.uuid);
                } else {
                    userChoicesSelect.removeActiveItems();
                }

                if (data.elementName) {
                    document.getElementById('name').value = data.elementName;
                } else {
                    document.getElementById('name').value = "";
                }
            }))
        .catch(error => toastService.error(error));
}

function updateTypeSelect(choices, search, types) {
    fetch(`/rest/relatable/autocomplete?types=${types}&search=${search}`)
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

function formReset() {
    const form = document.querySelector('form');
    form.reset();
}