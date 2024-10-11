const incidentService = new IncidentService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentService.init();
});

function IncidentService() {
    this.init = () => {
        let self = this;
        let targetId = "createIncidentDialog";
        fetchHtml(formUrl, targetId)
            .then(() => {
                self.initDatePickers(targetId);
                self.initAssetChoices(targetId);
            })
    }

    this.initDatePickers = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let pickers = dialog.querySelectorAll('.dateTimePicker');
        pickers.forEach(picker => {
            let id = picker.getAttribute('id');
            let buttonId = picker.parentElement.querySelector('button').getAttribute('id');
            initDatepicker(`#${buttonId}`, `#${id}` );
        })
    }

    this.initAssetChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let assetSelects = dialog.querySelectorAll('.assetSelect');
        assetSelects.forEach(select => {
            initAssetSelect(select.getAttribute('id'));
        })
    }
}
