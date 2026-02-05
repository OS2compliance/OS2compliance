import { refreshSchema, showSpinner, hideSpinner, bindEventListeners } from './schemaService.js';
import { setupFormValidation, submitFormViaFetch } from './formService.js';

const token = document.getElementsByName("_csrf")[0].getAttribute("content");

export function bindMeasureEventListeners() {
    bindEventListeners('.sortMeasureHigherBtn', sortMeasureHigher);
    bindEventListeners('.sortMeasureLowerBtn', sortMeasureLower);
    bindEventListeners('.editMeasure', editMeasure);
    bindEventListeners('.deleteMeasure', deleteMeasure);
}

async function sortMeasureHigher(event) {
    event.stopPropagation();
    const measureId = parseInt(this.dataset.measureid);
    await sortMeasure(measureId, 'up');
}

async function sortMeasureLower(event) {
    event.stopPropagation();
    const measureId = parseInt(this.dataset.measureid);
    await sortMeasure(measureId, 'down');
}

async function sortMeasure(measureId, direction) {
    showSpinner();

    try {
        const response = await fetch(`/rest/assets/measures/schema/measure/${measureId}/${direction}`, {
            method: "POST",
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': token
            }
        });

        if (!response.ok) {
            throw new Error('Kunne ikke flytte spørgsmålet');
        }

        await refreshSchema();
    } catch (error) {
        toastService.error(error.message);
        hideSpinner();
    }
}

function deleteMeasure(event) {
    event.stopPropagation();
    const measureId = parseInt(this.dataset.measureid);

    Swal.fire({
        title: 'Er du sikker?',
        text: 'Dette spørgsmål vil blive slettet',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Ja, slet!',
        cancelButtonText: 'Annuller'
    }).then((result) => {
        if (result.isConfirmed) {
            deleteData(`/rest/assets/measures/schema/measure/${measureId}/delete`)
                .then(() => {
                    toastService.info('Spørgsmålet er blevet slettet.');
                    refreshSchema();
                })
                .catch(defaultErrorHandler);
        }
    });
}

async function editMeasure(event) {
    event.stopPropagation();
    const measureId = parseInt(this.dataset.measureid);
    await showMeasureForm(measureId);
}

export async function showMeasureForm(measureId = null) {
    showSpinner();

    try {
        const url = measureId
            ? `/assets/measures/schema/measure/form?id=${measureId}`
            : '/assets/measures/schema/measure/form';

        const dialogId = measureId ? 'editMeasureDialog' : 'createMeasureDialog';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Kunne ikke hente formularen');
        }

        const html = await response.text();
        const dialog = document.getElementById(dialogId);
        dialog.innerHTML = html;

        const formId = measureId ? 'editMeasureForm' : 'createMeasureForm';
        const valuesSelect = document.getElementById(formId + 'ValuesSelect');
        if (valuesSelect) {
            new Choices(valuesSelect, {
                removeItemButton: true,
                searchEnabled: true,
                placeholderValue: 'Vælg svarmuligheder',
                searchPlaceholderValue: 'Søg...',
                noResultsText: 'Ingen resultater fundet',
                itemSelectText: 'Vælg',
            });
        }

        const modal = new bootstrap.Modal(dialog);
        setupFormValidation('measure', submitMeasureForm, dialogId);

        modal.show();
        hideSpinner();
    } catch (error) {
        toastService.error(error.message);
        hideSpinner();
    }
}

function submitMeasureForm(form) {
    const dialogId = form.closest('.modal').id;
    submitFormViaFetch(
        form,
        'createMeasureDialog',
        'editMeasureDialog',
        'Spørgsmålet er gemt',
        () => setupFormValidation('measure', submitMeasureForm, dialogId)
    );
}