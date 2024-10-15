const incidentService = new IncidentService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentService.init();
});

function IncidentService() {
    this.init = () => {
        this.fetchDialog(formUrl, "createIncidentDialog");
    }

    this.fetchDialog = (url, targetId) => {
        let self = this;
        return fetchHtml(url, targetId)
            .then(() => {
                self.initDatePickers(targetId);
                self.initAssetChoices(targetId);
                self.initUserChoices(targetId);
                self.initOrganizationChoices(targetId);
            })
    }

    this.editIncident = (targetId, incidentId) => {
        console.log(`Edit incident ${targetId} => ${incidentId}`)
        this.fetchDialog(`${formUrl}?id=${incidentId}`, targetId)
            .then(() => {
                let dialog = document.getElementById(targetId);
                let editDialog = new bootstrap.Modal(dialog);
                editDialog.show();
            })
    }

    this.fetchColumnName =  () => {
        return jsonCall('GET', restUrl + 'columns', null)
            .then((response) => {
                defaultResponseErrorHandler(response);
                return response.json();
            })
            .catch(defaultErrorHandler)
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
            initAssetSelect(select.getAttribute('id'), false);
        })
    }

    this.initUserChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let assetSelects = dialog.querySelectorAll('.userSelect');
        assetSelects.forEach(select => {
            initUserSelect(select.getAttribute('id'), false);
        })
    }

    this.initOrganizationChoices = (dialogId) => {
        let dialog = document.getElementById(dialogId);
        let assetSelects = dialog.querySelectorAll('.organizationSelect');
        assetSelects.forEach(select => {
            initOUSelect(select.getAttribute('id'), false);
        })
    }
}
