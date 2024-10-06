const incidentQuestionService = new IncidentQuestionService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentQuestionService.init();
});

function IncidentQuestionService() {

    this.init = () => {
    }

    this.initSelectList = (dialogId) => {
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

    this.selectionChanged = (selectElement) => {
        let value = selectElement.value;
        let form = selectElement.closest('form');
        let choiceListDiv = form.querySelector('.choiceListDiv');
        if (value === "CHOICE_LIST") {
            choiceListDiv.style.display = "inline-flex";
        } else {
            choiceListDiv.style.display = "none";
        }
    }

}
