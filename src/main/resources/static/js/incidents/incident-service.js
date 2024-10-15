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
        document.getElementById(targetId).innerText = '';
        this.fetchDialog(`${formUrl}?id=${incidentId}`, targetId)
            .then(() => {
                let dialog = document.getElementById(targetId);
                let editDialog = new bootstrap.Modal(dialog);
                editDialog.show();
            })
    }

    this.deleteIncident = (grid, targetId, name) => {
        Swal.fire({
            text: `Er du sikker på du vil slette hændelsen '${name}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                deleteData(`${restUrl}${targetId}`)
                    .then(response => grid.forceRender())
                    .catch(error => toastService.error(error));
            }
        });
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
