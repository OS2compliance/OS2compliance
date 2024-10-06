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

    this.sortQuestionHigher = (id) => {
        // TODO
    }

    this.sortQuestionLower = (id) => {
        // TODO
    }

    this.editQuestion = (id) => {
        // TODO
    }

    this.deleteQuestion = (id) => {
        // TODO
    }

}
