import { refreshSchema, showSpinner, hideSpinner, bindEventListeners } from './schemaService.js';
import { setupFormValidation, submitFormViaFetch } from './formService.js';

const token = document.getElementsByName("_csrf")[0].getAttribute("content");

export function bindCategoryEventListeners() {
    bindEventListeners('.sortCategoryHigherBtn', sortCategoryHigher);
    bindEventListeners('.sortCategoryLowerBtn', sortCategoryLower);
    bindEventListeners('.editCategory', editCategory);
    bindEventListeners('.deleteCategory', deleteCategory);
}

async function sortCategoryHigher(event) {
    event.stopPropagation();
    const categoryId = parseInt(this.dataset.categoryid);
    await sortCategory(categoryId, 'up');
}

async function sortCategoryLower(event) {
    event.stopPropagation();
    const categoryId = parseInt(this.dataset.categoryid);
    await sortCategory(categoryId, 'down');
}

async function sortCategory(categoryId, direction) {
    showSpinner();

    try {
        const response = await fetch(`/rest/assets/measures/schema/category/${categoryId}/${direction}`, {
            method: "POST",
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': token
            }
        });

        if (!response.ok) {
            throw new Error('Kunne ikke flytte kategorien');
        }

        await refreshSchema();
    } catch (error) {
        toastService.error(error.message);
        hideSpinner();
    }
}

function deleteCategory(event) {
    event.stopPropagation();
    const categoryId = parseInt(this.dataset.categoryid);

    Swal.fire({
        title: 'Er du sikker?',
        text: 'Alle spørgsmål i kategorien vil også blive slettet',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Ja, slet!',
        cancelButtonText: 'Annuller'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteData(`/rest/assets/measures/schema/category/${categoryId}/delete`)
                .then(() => {
                    toastService.info('Kategorien er blevet slettet.');
                    refreshSchema();
                })
                .catch(defaultErrorHandler);
        }
    });
}

async function editCategory(event) {
    event.stopPropagation();
    const categoryId = parseInt(this.dataset.categoryid);
    await showCategoryForm(categoryId);
}

export async function showCategoryForm(categoryId = null) {
    showSpinner();

    try {
        const url = categoryId
            ? `/assets/measures/schema/category/form?id=${categoryId}`
            : '/assets/measures/schema/category/form';

        const dialogId = categoryId ? 'editCategoryDialog' : 'createCategoryDialog';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Kunne ikke hente formularen');
        }

        const html = await response.text();
        const dialog = document.getElementById(dialogId);
        dialog.innerHTML = html;

        const modal = new bootstrap.Modal(dialog);
        setupFormValidation('category', submitCategoryForm, dialogId);

        modal.show();
        hideSpinner();
    } catch (error) {
        toastService.error(error.message);
        hideSpinner();
    }
}

function submitCategoryForm(form) {
    const dialogId = form.closest('.modal').id;
    submitFormViaFetch(
        form,
        'createCategoryDialog',
        'editCategoryDialog',
        'Kategorien er gemt',
        () => setupFormValidation('category', submitCategoryForm, dialogId)
    );
}