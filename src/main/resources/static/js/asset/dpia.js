
function checkboxChanged(event) {
    const sectionId = event.target.dataset.sectionid;
    const checked = event.target.checked;
    setFieldDpiaResponse(sectionId, "selected", !checked);
}

function mailReport() {
    var sendReportTo = document.getElementById('sendReportTo').value;
    var reportMessage = document.getElementById('reportMessage').value;
    var signReport = document.getElementById('signReport').checked;
    var data = {
                 "sendTo": sendReportTo,
                 "message": reportMessage,
                 "sign": signReport
               };

    postData(`/rest/assets/${assetId}/mailReport`, data).then((response) => {
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }
        toastService.info("Sendt");
        document.querySelector('#sendReportModal .btn-close').click();
    }).catch(error => {toastService.error(error)});
}

function initDpia() {

    // Bind foldable section events after content is loaded
//    const sectionRows = document.querySelectorAll('.dpiaSchemaSectionTr');
//    for (let i = 0; i < sectionRows.length; i++) {
//        var row = sectionRows[i];
//        var sectionId = row.dataset.sectionid;
//        handleSectionRow(sectionId);
//        row.addEventListener('click', sectionRowClicked, false);
//    }

//    const openedSection = sessionStorage.getItem(`openedDPIASectionId${assetId}`);
//    if (openedSection !== null && openedSection !== undefined) {
//        handleSectionRow(openedSection);
//    }

    // init send to select
//    let responsibleSelect = document.getElementById('sendReportTo');
//    if(responsibleSelect !== null) {
//        choiceService.initUserSelect('sendReportTo');
//    }
}
