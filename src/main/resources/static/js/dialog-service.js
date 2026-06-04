const WARNING_ICON = `<svg viewBox="0 0 52 52" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style="width:52px;height:52px">
    <path d="M26 4L48 44H4L26 4Z" fill="#f0ad4e" stroke="#d4892a" stroke-width="2.5" stroke-linejoin="round"/>
    <rect x="23.5" y="19" width="5" height="13" rx="2.5" fill="#fff"/>
    <rect x="23.5" y="35" width="5" height="5" rx="2.5" fill="#fff"/>
</svg>`;

// Only what Bootstrap cannot do: reset <dialog> defaults and style the backdrop
const STYLES = `
    #dsDialog {
        border: none;
        border-radius: var(--bs-border-radius-lg);
        padding: 0;
        max-width: 520px;
        width: 90%;
        box-shadow: var(--bs-box-shadow-lg, 0 8px 32px rgba(0,0,0,.2));
    }
    #dsDialog::backdrop {
        background: rgba(0, 0, 0, 0.45);
    }
`;

let dialogEl = null;
let contentEl = null;

function init() {
    if (dialogEl) {
        return;
    }

    const style = document.createElement('style');
    style.textContent = STYLES;
    document.head.appendChild(style);

    dialogEl = document.createElement('dialog');
    dialogEl.id = 'dsDialog';

    contentEl = document.createElement('div');
    contentEl.id = 'dsContent';
    dialogEl.appendChild(contentEl);

    document.body.appendChild(dialogEl);
}

export async function openDialog(url, initFunction = null) {
    if (!url?.trim()) {
        console.warn('openDialog called with no url');
        return;
    }

    init();

    if (dialogEl.open) {
        dialogEl.close();
    }

    await networkService.GetFragment(url, contentEl);

    if (typeof initFunction === 'function') {
        initFunction(contentEl);
    }

    dialogEl.showModal();
}

export function closeDialog() {
    dialogEl?.close();
}

export function initSubmitButton(
    container,
    url,
    errorMessage = 'En fejl opstod og oplysningerne blev ikke gemt',
    customValidationFunction = null,
    buttonSelector = '.confirmBtn'
) {
    const confirmButton = container.querySelector(buttonSelector);
    confirmButton?.addEventListener('click', async () => {
        await submitData(container, url, errorMessage, customValidationFunction);
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
    customValidationFunction = null
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

    location.reload();
}

// Use confirm if the intention is to warn the user of something and allow them to either continue or cancel
export async function confirm(options = {}) {
    const result = await showStatic({
        title: options.title,
        text: options.text,
        icon: options.icon,
        confirmButtonText: options.confirmButtonText || 'Ja',
        cancelButtonText: options.cancelButtonText || 'Nej',
        showCancel: true,
    });
    return result.isConfirmed;
}

// Use alert if the intention is to inform the user of something without them needing to take any actions
export async function alert(options = {}) {
    if (typeof options === 'string') {
        options = { text: options };
    }

    return showStatic({
        title: options.title,
        text: options.text,
        icon: options.icon,
        confirmButtonText: options.confirmButtonText || 'OK',
        showCancel: false,
    });
}

function showStatic({ title, text, icon, confirmButtonText, cancelButtonText, showCancel }) {
    init();

    const iconEl = document.createElement('div');
    iconEl.className = 'mb-3';
    if (icon === 'warning') {
        iconEl.insertAdjacentHTML('afterbegin', WARNING_ICON);
    }
    iconEl.hidden = !icon;

    const titleEl = document.createElement('p');
    titleEl.className = 'fw-semibold fs-5 mb-1';
    titleEl.textContent = title || '';
    titleEl.hidden = !title;

    const textEl = document.createElement('p');
    textEl.className = 'text-secondary mb-4';
    textEl.textContent = text || '';

    const cancelBtn = document.createElement('button');
    cancelBtn.type = 'button';
    cancelBtn.className = 'btn btn-success';
    cancelBtn.textContent = cancelButtonText || 'Annuller';
    cancelBtn.hidden = !showCancel;

    const confirmBtn = document.createElement('button');
    confirmBtn.type = 'button';
    confirmBtn.className = 'btn btn-danger';
    confirmBtn.textContent = confirmButtonText || 'OK';

    const actions = document.createElement('div');
    actions.className = 'd-flex justify-content-center gap-2';
    actions.append(cancelBtn, confirmBtn);

    const inner = document.createElement('div');
    inner.className = 'p-4 text-center';
    inner.append(iconEl, titleEl, textEl, actions);

    contentEl.replaceChildren(inner);

    return new Promise((resolve) => {
        function done(confirmed) {
            dialogEl.close();
            resolve({ isConfirmed: confirmed });
        }

        confirmBtn.addEventListener('click', () => done(true));
        cancelBtn.addEventListener('click', () => done(false));

        // Native ESC fires 'cancel' on the <dialog> element
        dialogEl.addEventListener('cancel', () => resolve({ isConfirmed: false }), { once: true });

        dialogEl.showModal();
    });
}
