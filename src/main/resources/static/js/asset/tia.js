import { debounce } from "../debounce-service.js";
import { confirm } from "../dialog-service.js";

let bsCollapse;
let tiaChoiceElements = [];
const SAVE_DEBOUNCE_MS = 800;

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

async function saveTia() {
    const form = document.getElementById('tiaForm');
    if (!form) {
        return;
    }

    try {
        const response = await fetch(form.action, {
            method: 'POST',
            body: new FormData(form)
        });
        if (response.ok) {
            toastService.info('Gemt');
            return true;
        }
        toastService.error('Kunne ikke gemme');
    } catch {
        toastService.error('Kunne ikke gemme');
    }
    return false;
}

function setTiaAcceptedState(locked) {
    document.getElementById('tiaView').querySelectorAll('select, textarea, input:not([type="hidden"])').forEach(e => {
        e.disabled = locked;
    });
    tiaChoiceElements.forEach(e => locked ? e.disable() : e.enable());
    document.getElementById('removeAcceptanceRow').hidden = !locked;

    const tiaLinkEditBtn = document.getElementById('tiaLinkEditBtn');
    if (tiaLinkEditBtn) {
        tiaLinkEditBtn.disabled = locked;
    }
}

function lockTiaForm() {
    setTiaAcceptedState(true);
}

function unlockTiaForm() {
    setTiaAcceptedState(false);
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

    if (!changeableAsset) {
        document.getElementById('tiaTab').querySelectorAll('select, textarea, input:not([type="hidden"])').forEach(e => { e.disabled = true; });
        tiaChoiceElements.forEach(e => e.disable());
        return;
    }

    const form = document.getElementById('tiaForm');
    const debouncedSave = debounce(() => saveTia(), SAVE_DEBOUNCE_MS);

    for (const elem of form.elements) {
        if (elem.type === 'hidden' || elem.tagName === 'BUTTON' || !elem.name || elem.name === 'tia.accepted' || elem.id === 'setTiaOptOutCheckbox') {
            continue;
        }

        if (elem.tagName === 'TEXTAREA') {
            elem.addEventListener('input', debouncedSave);
        } else {
            elem.addEventListener('change', () => saveTia());
        }
    }

    const acceptedCheckbox = form.querySelector('input[name="tia.accepted"][type="checkbox"]');
    if (acceptedCheckbox) {
        acceptedCheckbox.addEventListener('change', async () => {
            const saved = await saveTia();
            if (saved) {
                location.reload();
            }
        });
    }

    const removeAcceptanceBtn = document.getElementById('removeAcceptanceBtn');
    if (removeAcceptanceBtn) {
        removeAcceptanceBtn.addEventListener('click', async () => {
            const confirmed = await confirm({
                text: 'Er du sikker på, at du vil fjerne godkendelsen?',
                icon: 'warning',
            });
            if (!confirmed) {
                return;
            }

            if (acceptedCheckbox) {
                acceptedCheckbox.checked = false;
            }
            const acceptComment = form.querySelector('[name="tia.acceptedComment"]');
            if (acceptComment) {
                acceptComment.value = '';
            }
            document.getElementById('acceptDateRow').hidden = true;

            unlockTiaForm();
            await saveTia();
        });
    }

    if (tiaAccepted) {
        lockTiaForm();
    }

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
