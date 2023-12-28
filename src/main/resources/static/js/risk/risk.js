
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

function initRegisterSelect() {
    const registerSelect = document.getElementById('registerSelect');
    const registerChoices = initSelect(registerSelect);
    updateTypeSelect(registerChoices, "", "REGISTER");
    registerSelect.addEventListener("search",
        function(event) {
            updateTypeSelect(registerChoices, event.detail.value, "REGISTER");
        },
        false,
    );
    return registerChoices;
}

function initAssetSelect() {
    const assetSelect = document.getElementById('assetSelect');
    const assetChoices = initSelect(assetSelect);
    updateTypeSelect(assetChoices, "", "ASSET");
    assetSelect.addEventListener("search",
        function(event) {
            updateTypeSelect(assetChoices, event.detail.value, "ASSET");
        },
        false,
    );
    return assetChoices;
}


function typeChanged(selectedType) {
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
    registerChoicesSelect.removeActiveItems();
    assetChoicesSelect.removeActiveItems();
}

function loadAssetSection() {
    const selectedAsset = document.getElementById("assetSelect").value;
    fetch( `/rest/risks/asset?assetId=${selectedAsset}`)
        .then(response => response.json()
            .then(data => {
                var user = data.user;
                if (user.uuid != null) {
                    userChoicesSelect.setChoiceByValue(user.uuid);
                } else {
                    userChoicesSelect.removeActiveItems();
                }

                if (data.elementName != null) {
                    document.getElementById('name').value = data.elementName;
                } else {
                    document.getElementById('name').value = "";
                }

                // set text in table
                document.getElementById("RF").innerHTML=data.rf == 0 ? "" : data.rf;
                document.getElementById("OF").innerHTML=data.of == 0 ? "" : data.of;
                document.getElementById("RI").innerHTML=data.ri == 0 ? "" : data.ri;
                document.getElementById("OI").innerHTML=data.oi == 0 ? "" : data.oi;
                document.getElementById("RT").innerHTML=data.rt == 0 ? "" : data.rt;
                document.getElementById("OT").innerHTML=data.ot == 0 ? "" : data.ot;

                document.getElementById("inheritRow").style.display = '';
            }))
        .catch(error => toastService.error(error));
}

function loadRegisterResponsible() {
    var selectedRegister = document.getElementById("registerSelect").value;
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

function sendEmailChanged(checked) {
    document.getElementById('sendEmail').value = checked;
}

function validateChoicesAndCheckboxesRisk(...choiceList) {
    let result = true;
    for (let i = 0; i < choiceList.length; i++) {
        if (!checkInputField(choiceList[i])) {
            result = false;
        }
    }

    var registered = document.getElementById("registered");
    var organisation = document.getElementById("organisation");
    if (!registered.checked && !organisation.checked) {
        registered.classList.add('is-invalid');
        organisation.classList.add('is-invalid');
        document.getElementById("checkboxError").classList.add('show');
        result = false;
    } else {
        registered.classList.remove('is-invalid');
        organisation.classList.remove('is-invalid');
        document.getElementById("checkboxError").classList.remove('show');
    }

    return result;
}

let registerChoicesSelect, assetChoicesSelect, userChoicesSelect, ouChoicesSelect;
function createFormLoaded() {
    registerChoicesSelect = initRegisterSelect();
    assetChoicesSelect = initAssetSelect();
    userChoicesSelect = initUserSelect("userSelect");
    ouChoicesSelect = initOUSelect("ouSelect");
    typeChanged(document.getElementById("threatAssessmentType").value);

    document.getElementById('threatAssessmentType').addEventListener('change', function() {
        typeChanged(this.value);
    });

    assetChoicesSelect.passedElement.element.addEventListener('change', function() {
        loadAssetSection();
    });
    registerChoicesSelect.passedElement.element.addEventListener('change', function() {
        loadRegisterResponsible();
    });

    document.getElementById('sendEmailcheckbox').addEventListener('change', function() {
        sendEmailChanged(this.checked);
    });

    initFormValidationForForm("createRiskModal", () => validateChoicesAndCheckboxesRisk(userChoicesSelect, ouChoicesSelect));
}
