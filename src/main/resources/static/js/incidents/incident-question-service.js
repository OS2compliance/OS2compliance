const incidentQuestionService = new IncidentQuestionService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentQuestionService.init();
});

function IncidentQuestionService() {

    this.init = () => {
    }

    this.initFormSelectList = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let choices = dialog.querySelectorAll(".choices");
        [...choices].forEach(c => {
            new Choices(c, {
                allowHTML: true,
                delimiter: ',',
                removeItemButton: true,
                addItemText: (value) => {
                    return `Tryk enter for at tilføje <b>"${value}"</b>`;
                },
            });
        });
        let formId = dialog.querySelector('form').getAttribute('id');
        initFormValidationForForm(formId);
    }

    this.formIncidentSelectionChanged = (selectElement) => {
        let value = selectElement.value;
        let form = selectElement.closest('form');
        let choiceListDiv = form.querySelector('.choiceListDiv');
        if (value === "CHOICE_LIST") {
            choiceListDiv.style.display = "inline-flex";
        } else {
            choiceListDiv.style.display = "none";
        }
    }

    this.gridFindPreviousRow = (grid, id) => {
        let lastRow = null;
        if (grid.data) {
            for (let i = 0; i<grid.data.length; ++i) {
                const row = grid.data[i];
                if (row.id === id) {
                    break;
                }
                lastRow = row;
            }
        }
        return lastRow;
    }

    this.gridFindNextRow = (grid, id) => {
        let lastRow = null;
        if (grid.data) {
            for (let i = grid.data.length-1; i>=0; --i) {
                const row = grid.data[i];
                if (row.id === id) {
                    break;
                }
                lastRow = row;
            }
        }
        return lastRow;
    }

    this.sortQuestionUp = (grid, id) => {
        // let grid = document.getElementById(gridId);
        postData(`${restUrl}/${id}/up`)
            .then(response => grid.forceRender())
            .catch(error => toastService.error(error));
    }

    this.sortQuestionDown = (grid, id) => {
        postData(`${restUrl}/${id}/down`)
            .then(response => grid.forceRender())
            .catch(error => toastService.error(error));
    }

    this.editQuestion = (dialogId, id) => {
        fetchHtml(`${formUrl}?id=${id}`, dialogId).then(() => {
            let dialog = document.getElementById(dialogId);
            incidentQuestionService.initFormSelectList(dialogId);
            let editDialog = new bootstrap.Modal(dialog);
            editDialog.show();
        });
    }

    this.deleteQuestion = (grid, id, question) => {
        Swal.fire({
            text: `Er du sikker på du vil slette spørgsmålet '${question}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                deleteData(`${restUrl}/${id}`)
                    .then(response => grid.forceRender())
                    .catch(error => toastService.error(error));
            }
        });
    }

}
