import { refreshSchema } from './schemaService.js';

const token = document.getElementsByName("_csrf")[0].getAttribute("content");

export function setupFormValidation(type, submitCallback, dialogId) {
    const dialog = document.getElementById(dialogId);
    if (!dialog) return;

    const submitBtn = dialog.querySelector(`.submit-${type}-form`);
    const form = dialog.querySelector('.needs-validation');

    if (!submitBtn || !form) return;

    submitBtn.addEventListener('click', function(e) {
        e.preventDefault();

        if (form.checkValidity()) {
            submitCallback(form);
        } else {
            form.classList.add('was-validated');
        }
    });
}

export async function submitFormViaFetch(form, createDialogId, editDialogId, successMessage, setupCallback) {
    const submitBtn = form.closest('.modal-content').querySelector('[class*="submit-"]');
    const formData = new FormData(form);

    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Gemmer...';

    try {
        const response = await fetch(form.action, {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            body: formData
        });

        if (response.redirected) {
            // Success - close modal and refresh
            const modalElement = form.closest('.modal');
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) {
                modal.hide();
            }
            toastService.info(successMessage);
            await refreshSchema();
        } else {
            // Error - reload modal content with error message
            const html = await response.text();
            const dialog = form.closest('.modal').id === createDialogId
                ? document.getElementById(createDialogId)
                : document.getElementById(editDialogId);
            dialog.innerHTML = html;
            setupCallback();
        }
    } catch (error) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Gem';
        toastService.error('Der opstod en fejl: ' + error.message);
    }
}