
export default function IncidentQuestionService() {
    const formUrl = "/incidents/questionForm"
    const restUrl = "/rest/incidents/questions"

    this.init = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        this.initFormSelectList(dialog);
        this.updateColumNameVisibility(dialogId);
        let form = dialog.querySelector('form');
        form.addEventListener('submit', (event) => {
            this.submit(event, form);
        })
    }

    this.toggleColumnVisibility = (element) => {
        let closest = element.closest(".modal");
        this.updateColumNameVisibility(closest.getAttribute("id"));
    }

    this.updateColumNameVisibility = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let showColumn = dialog.querySelector(".indexColumn");
        let columnSection = dialog.querySelector(".columnNameSection");
        columnSection.style.display = showColumn.checked ? "inline-flex" : "none";
    }

    this.submit = (event, form) => {
        event.preventDefault();
        let showColumn = form.querySelector(".indexColumn");
        if (!showColumn.checked) {
            let columnNameInput = form.querySelector('.columnName');
            columnNameInput.value = '';
        }
        form.submit();
    }

    this.initFormSelectList = (dialog) => {
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
        if (value === "CHOICE_LIST" || value === "CHOICE_LIST_MULTIPLE") {
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
        postData(`${restUrl}/${id}/up`)
            .then(response => grid.forceRender())
            .catch(error => toastService.error(error));
    }

    this.sortQuestionDown = (grid, id) => {
        postData(`${restUrl}/${id}/down`)
            .then(response => grid.forceRender())
            .catch(error => toastService.error(error));
    }

    this.createQuestion = (dialogId) => {
        fetchHtml(`${formUrl}`, dialogId).then(() => {
            this.init('createQuestionDialog');
        });
    }

    this.editQuestion = (dialogId, id) => {
        fetchHtml(`${formUrl}?id=${id}`, dialogId).then(() => {
            let dialog = document.getElementById(dialogId);
            this.init(dialogId);
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
