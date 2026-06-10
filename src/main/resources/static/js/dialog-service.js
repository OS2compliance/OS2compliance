// The dialog markup, icons and styling live in templates/fragments/dialog-service.html,
// included on every page through templates/fragments/footer.html. This service only fills
// in the caller's content and wires up the buttons.

function getDialog(id) {
    const dialogEl = document.getElementById(id);
    if (!dialogEl) {
        console.error(`#${id} not found - is the dialog-service fragment included in the footer?`);
    }
    return dialogEl;
}

export async function openDialog(url, initFunction = null) {
    if (!url?.trim()) {
        console.warn('openDialog called with no url');
        return;
    }

    const dialogEl = getDialog('dsContentDialog');
    if (!dialogEl) {
        return;
    }
    const contentEl = dialogEl.querySelector('#dsContent');

    if (dialogEl.open) {
        dialogEl.close();
    }

    try {
        await networkService.GetFragment(url, contentEl);
    } catch (e) {
        console.error(e);
        toastService.error('Indholdet kunne ikke hentes');
        return;
    }

    if (typeof initFunction === 'function') {
        initFunction(contentEl);
    }

    dialogEl.showModal();
}

export function closeDialog() {
    getDialog('dsContentDialog')?.close();
}

export function initSubmitButton(
    container,
    url,
    errorMessage = 'En fejl opstod og oplysningerne blev ikke gemt',
    customValidationFunction = null,
    buttonSelector = '.confirmBtn',
    onSuccess = () => location.reload()
) {
    const confirmButton = container.querySelector(buttonSelector);
    confirmButton?.addEventListener('click', async () => {
        await submitData(container, url, errorMessage, customValidationFunction, onSuccess);
    });
}

export function formToJson(formData) {
    const jsonObject = {};
    for (const [key, value] of formData) {
        if (key in jsonObject) {
            jsonObject[key] = Array.isArray(jsonObject[key])
                ? [...jsonObject[key], value]
                : [jsonObject[key], value];
        } else {
            jsonObject[key] = value;
        }
    }
    return jsonObject;
}

async function submitData(
    container,
    url,
    errorMessage = 'En fejl opstod og oplysningerne blev ikke gemt',
    customValidationFunction = null,
    onSuccess = () => location.reload()
) {
    const form = container.querySelector('form');

    let valid = form.checkValidity();
    if (typeof customValidationFunction === 'function') {
        valid = valid && customValidationFunction();
    }
    if (!valid) {
        form.classList.add('was-validated');
        return;
    }

    const json = formToJson(new FormData(form));

    try {
        await networkService.Post(url, json);
    } catch (e) {
        console.error(e);
        toastService.error(errorMessage);
        return;
    }

    onSuccess();
}

// Use confirm if the intention is to warn the user of something and allow them to either continue or cancel
export async function showConfirm(options = {}) {
    const result = await showStatic('dsConfirmDialog', {
        title: options.title,
        text: options.text,
        icon: options.icon,
        confirmButtonText: options.confirmButtonText || 'Ja',
        confirmButtonClass: options.confirmButtonClass || 'btn-danger',
        cancelButtonText: options.cancelButtonText || 'Nej',
        cancelButtonClass: options.cancelButtonClass || 'btn-success',
        showCancel: true,
    });
    return result.isConfirmed;
}

// Use alert if the intention is to inform the user of something without them needing to take any actions
export async function showAlert(options = {}) {
    if (typeof options === 'string') {
        options = { text: options };
    }

    return await showStatic('dsAlertDialog', {
        title: options.title,
        text: options.text,
        icon: options.icon,
        confirmButtonText: options.confirmButtonText || 'OK',
        confirmButtonClass: options.confirmButtonClass || 'btn-primary',
        showCancel: false,
    });
}

function showStatic(dialogId, { title, text, icon, confirmButtonText, confirmButtonClass, cancelButtonText, cancelButtonClass, showCancel }) {
    const dialogEl = getDialog(dialogId);
    if (!dialogEl) {
        return Promise.resolve({ isConfirmed: false });
    }

    if (dialogEl.open) {
        dialogEl.close();
    }

    const iconEl = dialogEl.querySelector('.ds-icon');
    const warningIcon = dialogEl.querySelector('.ds-icon-warning');
    const infoIcon = dialogEl.querySelector('.ds-icon-info');
    warningIcon.hidden = icon !== 'warning';
    infoIcon.hidden = icon !== 'info';
    iconEl.hidden = !icon;

    const titleEl = dialogEl.querySelector('.ds-title');
    titleEl.textContent = title || '';
    titleEl.hidden = !title;

    const textEl = dialogEl.querySelector('.ds-text');
    textEl.textContent = text || '';
    textEl.hidden = !text;

    const confirmBtn = dialogEl.querySelector('.ds-confirm');
    confirmBtn.className = `ds-confirm btn ${confirmButtonClass || 'btn-success'} btn-lg`;
    confirmBtn.textContent = confirmButtonText || 'OK';

    const cancelBtn = dialogEl.querySelector('.ds-cancel');
    if (cancelBtn) {
        cancelBtn.className = `ds-cancel btn ${cancelButtonClass || 'btn-danger'} btn-lg`;
        cancelBtn.textContent = cancelButtonText || 'Annuller';
        cancelBtn.hidden = !showCancel;
    }

    return new Promise((resolve) => {
        // Buttons are reused across calls, so tie this call's listeners to an
        // AbortController and drop them all once the dialog resolves.
        const controller = new AbortController();
        const { signal } = controller;

        function done(confirmed) {
            controller.abort();
            dialogEl.close();
            resolve({ isConfirmed: confirmed });
        }

        confirmBtn.addEventListener('click', () => done(true), { signal });
        cancelBtn?.addEventListener('click', () => done(false), { signal });

        // Native ESC fires cancel on the dialog element
        dialogEl.addEventListener('cancel', () => done(false), { signal });

        dialogEl.showModal();
    });
}
