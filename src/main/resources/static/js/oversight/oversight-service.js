const oversightService = new OversightService();
document.addEventListener("DOMContentLoaded", function(event) {
    oversightService.init();
});

function OversightService() {

    this.init = () => {
    }

    this.showOversightModal = () => {
        this.initOversightModal().then(() => oversightDialog.show());
    }

    this.initOversightModal = (oversightId, entityType, entityId) => {
        if (entityType === undefined) {
            let isAsset = window.location.href.indexOf('assets') > 0;
            entityType = isAsset ? 'asset' : 'supplier';
            entityId = isAsset ? assetId : supplierId;
        }

        let params = new URLSearchParams();
        if (oversightId) {
            params.append('id', oversightId);
        }
        return fetchHtml(`${oversightUrl}/${entityId}/${entityType}?${params}`, 'oversightDialog')
            .then(() => {
                //Init modal
                oversightDialog = new bootstrap.Modal(document.getElementById('oversightDialog'), {
                    keyboard: false
                });

                //Init date pickers
                const datepickerOversight = MCDatepicker.create({
                    el: '#oversightDateId',
                    autoClose: true,
                    dateFormat: 'dd/mm-yyyy',
                    selectedDate: new Date(),
                    closeOnBlur: true,
                    firstWeekday: 1,
                    customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
                    customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
                    customClearBTN: "Ryd",
                    customCancelBTN: "Annuller"
                });
                document.querySelector("#oversightDateBtn").addEventListener("click", () => {
                    datepickerOversight.open();
                });


                let oversightInspectionBtn = document.querySelector("#oversightInspectionDateBtn");
                if (oversightInspectionBtn !== null) {
                    const inspectionDateOversightModal = MCDatepicker.create({
                        el: '#oversightInspectionDateInput',
                        autoClose: true,
                        dateFormat: 'dd/mm-yyyy',
                        selectedDate: new Date(),
                        closeOnBlur: true,
                        firstWeekday: 1,
                        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
                        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
                        customClearBTN: "Ryd",
                        customCancelBTN: "Annuller"
                    });
                    oversightInspectionBtn.addEventListener("click", () => {
                        inspectionDateOversightModal.open();
                    });
                }

                //choices.js
                const oversightSelect = document.getElementById('oversightUserSelect');
                oversightResponsibleUserChoices = initSelect(oversightSelect);
                choiceService.updateUsers(oversightResponsibleUserChoices, "");
                oversightSelect.addEventListener("search",
                    function(event) {
                        choiceService.updateUsers(oversightResponsibleUserChoices, event.detail.value);
                    },
                    false,
                );
                oversightResponsibleUserChoices.passedElement.element.addEventListener('change', function() {
                    checkInputField(oversightResponsibleUserChoices);
                });
            })
            .catch(defaultErrorHandler)
    }

    this.editOversight = (elem) => {
        const oversightId = elem.dataset.oversightid;
        this.initOversightModal(oversightId).then(() => oversightDialog.show());
    }

    this.deleteOversight = (elem) => {
        const oversightId = elem.dataset.oversightid;
        deleteData(`${oversightRestUrl}/${oversightId}`)
            .then(() => window.location.reload());
    }
}
