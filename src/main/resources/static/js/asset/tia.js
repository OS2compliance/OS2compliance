let bsCollapse;
let tiaChoiceElements = [];

function initRegisteredCategoriesChoices(values) {
    const select = document.getElementById('tia.registeredCategories');
    const choice = initSelect(select);
    choice.setChoices(values, 'identifier', 'caption', true);
    for (const option of document.querySelectorAll('#tia\\.registeredCategories option')) {
        if (chosenCategories !== null && chosenCategories.includes(option.value)) {
            option.setAttribute('selected', 'selected');
        }
    }
    tiaChoiceElements.push(choice);
}

function initInformationTypeChoices(values) {
    const select = document.getElementById('tia.informationTypes');
    const choice = initSelect(select);
    choice.setChoices(values, 'identifier', 'caption', true);
    for (const option of document.querySelectorAll('#tia\\.informationTypes option')) {
        if (chosenInformationTypes !== null && chosenInformationTypes.includes(option.value)) {
            option.setAttribute('selected', 'selected');
        }
    }
    tiaChoiceElements.push(choice);
}

function handleSendDataToOtherSuppliers() {
    const element = document.getElementById('tia.forwardInformationToOtherSuppliers');
    if (element.value === "YES") {
        bsCollapse.show();
    } else {
        bsCollapse.hide();
    }
}

export function setTIAEditState(enabled) {
    const rootElement = document.getElementById('tiaTab');

    if (enabled) {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = false;

            if (elem.tagName === "A") {
                elem.hidden = true;
                if (elem.nextElementSibling) {
                    elem.nextElementSibling.hidden = false;
                }
            }

            if (elem.classList.contains("datepicker")) {
                elem.parentElement.hidden = false;
                if (elem.parentElement.nextElementSibling) {
                    elem.parentElement.nextElementSibling.hidden = true;
                }
            }
        });

        tiaChoiceElements.forEach((e) => e.enable());

        if (changeableAsset) {
            document.getElementById('saveTIABtn').hidden = false;
            document.getElementById('editTIABtn').hidden = true;
            document.getElementById('cancelTIABtn').hidden = false;
        }
    } else {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = true;

            if (elem.tagName === "A") {
                elem.hidden = false;
                elem.nextElementSibling.hidden = true;
            }

            if (elem.classList.contains("datepicker")) {
                if (elem.value == null || elem.value === "") {
                    elem.parentElement.hidden = true;
                    elem.parentElement.nextElementSibling.hidden = false;
                }
            }
        });

        tiaChoiceElements.forEach((e) => e.disable());

        if (changeableAsset) {
            document.getElementById('saveTIABtn').hidden = true;
            document.getElementById('editTIABtn').hidden = false;
            document.getElementById('cancelTIABtn').hidden = true;
        }

        document.getElementById("tiaForm").reset();
    }
}

function tiaLinkEditStart() {
    document.getElementById('tiaLinkView').classList.add('d-none');
    document.getElementById('tiaLinkEditGroup').classList.remove('d-none');
    document.getElementById('tiaLinkInput').focus();
}

function tiaLinkSave() {
    const input = document.getElementById('tiaLinkInput');
    let value = input.value.trim();

    if (value) {
        value = 'https://' + value.replace(/^https?:\/\//i, '');
        input.value = value;
    }

    assetDetailsService.setField('tiaLink', value);

    const anchor = document.getElementById('tiaLinkAnchor');
    anchor.href = value;
    anchor.textContent = value || 'Ingen link';

    document.getElementById('tiaLinkView').classList.remove('d-none');
    document.getElementById('tiaLinkEditGroup').classList.add('d-none');
}

function tiaLinkCancel() {
    const anchor = document.getElementById('tiaLinkAnchor');
    document.getElementById('tiaLinkInput').value = anchor.textContent === 'Ingen link' ? '' : anchor.textContent;
    document.getElementById('tiaLinkView').classList.remove('d-none');
    document.getElementById('tiaLinkEditGroup').classList.add('d-none');
}

export function initTia() {
    bsCollapse = new bootstrap.Collapse(document.getElementById('details'), { toggle: false });

    initRegisteredCategoriesChoices(registeredCategories);
    initInformationTypeChoices(informationChoices1.concat(informationChoices2));

    handleSendDataToOtherSuppliers();
    setTIAEditState(false);

    document.getElementById('tia.forwardInformationToOtherSuppliers')
        .addEventListener('change', handleSendDataToOtherSuppliers);

    document.getElementById('setTiaOptOutCheckbox')
        .addEventListener('change', function () {
            assetDetailsService.setTiaOptOut(this);
        });

    document.getElementById('tiaOptOutText')
        .addEventListener('change', function () {
            assetDetailsService.updateTiaOptOutReason(this);
        });

    const editTIABtn = document.getElementById('editTIABtn');
    const cancelTIABtn = document.getElementById('cancelTIABtn');

    if (editTIABtn) {
        editTIABtn.addEventListener('click', () => {
            if (tiaAccepted && !confirm('TIA er godkendt. Hvis du redigerer, vil godkendelsen blive fjernet. Vil du fortsætte?')) {
                return;
            }
            setTIAEditState(true);
        });
    }
    if (cancelTIABtn) {
        cancelTIABtn.addEventListener('click', () => setTIAEditState(false));
    }

    const tiaLinkEditBtn = document.getElementById('tiaLinkEditBtn');
    const tiaLinkSaveBtn = document.getElementById('tiaLinkSaveBtn');
    const tiaLinkCancelBtn = document.getElementById('tiaLinkCancelBtn');
    const tiaLinkInput = document.getElementById('tiaLinkInput');

    if (tiaLinkEditBtn) {
        tiaLinkEditBtn.addEventListener('click', tiaLinkEditStart);
    }
    if (tiaLinkSaveBtn) {
        tiaLinkSaveBtn.addEventListener('click', tiaLinkSave);
    }
    if (tiaLinkCancelBtn) {
        tiaLinkCancelBtn.addEventListener('click', tiaLinkCancel);
    }

    if (tiaLinkInput) {
        tiaLinkInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                tiaLinkSave();
            }
        });
    }
}
