const incidentService = new IncidentService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentService.init();
});

function IncidentService() {
    this.init = () => {
        let self = this;
        fetchHtml(formUrl, "createIncidentDialog")
            .then(() => self.initDatePickers("createIncidentDialog"))
    }

    this.initDatePickers = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let pickers = dialog.querySelectorAll('.dateTimePicker');
        pickers.forEach(picker => {
            let id = picker.getAttribute('id');
            let buttonId = picker.parentElement.querySelector('button').getAttribute('id');
            initDatepicker(`#${id}`, `#${buttonId}`);
        })
    }
}
