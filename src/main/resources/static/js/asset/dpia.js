function setFieldDpiaResponse(id, fieldName, value) {
    var data = {
        "id": id,
        "fieldName": fieldName,
        "value": value
    };
    putData(`/rest/assets/${assetId}/dpia/response/setfield`, data)
        .then(defaultResponseHandler)
        .catch(defaultErrorHandler);
}

function setFieldDpia(fieldName, value) {
    var data = {
        "fieldName": fieldName,
        "value": value
    };
    putData(`/rest/assets/${assetId}/dpia/setfield`, data)
        .then(defaultResponseHandler)
        .catch(defaultErrorHandler);
}

function sectionRowClicked() {
    var sectionId = this.dataset.sectionid;
    sessionStorage.setItem(`openedDPIASectionId${assetId}`, sectionId);
    handleSectionRow(sectionId);
}

function checkboxChanged(event) {
    const sectionId = event.target.dataset.sectionid;
    const checked = event.target.checked;
    setFieldDpiaResponse(sectionId, "selected", !checked);
}

function handleSelectThreatAssessmentCheckboxes(event) {
    let selected = '';
    const selectThreatAssessmentCheckboxes = document.querySelectorAll('.selectThreatAssessment');
    for (let i = 0; i < selectThreatAssessmentCheckboxes.length; i++) {
        let checkbox = selectThreatAssessmentCheckboxes[i];
        const threatId = checkbox.dataset.threatid;
        const checked = checkbox.checked;
        if (checked) {
            selected = selected + threatId + ",";
        }
    }
    setFieldDpia("checkedThreatAssessmentIds", selected.slice(0, -1));
}

function handleSectionRow(sectionId) {
    var show = false;
    var icon = document.getElementById("sectionIcon" + sectionId);
    const belongingRows = document.querySelectorAll('.sectionRow' + sectionId);

    for (var i = 0; i < belongingRows.length; i++) {
        var elem = belongingRows[i];
        if (elem.hidden) {
            if (i == 0) {
                show = true;
            }
            elem.hidden = false;
        } else {
            elem.hidden = true;
        }
    }

    if (show) {
        icon.classList.add("pli-arrow-up");
        icon.classList.remove("pli-arrow-down");
    } else {
        icon.classList.remove("pli-arrow-up");
        icon.classList.add("pli-arrow-down");
    }
}

function editQuestion() {
    var questionId = parseInt(this.dataset.questionid);
    fetch(`${questionFormUrl}?id=${questionId}`)
        .then(response => response.text()
            .then(data => {
                let dialog = document.getElementById('editQuestionDialog');
                dialog.innerHTML = data;
                editDialog = new bootstrap.Modal(document.getElementById('editQuestionDialog'));

                let editorTextArea = document.getElementById('editInstructions');
                window.CreateCkEditor(editorTextArea, editor => {
                    editor.editing.view.document.on('blur', () => {
                        editorTextArea.value = editor.getData();
                    });
                });

                editDialog.show();
            }))
        .catch(error => toastService.error(error));
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
    let editors = document.querySelectorAll('.responses');
    for (let i = 0; i < editors.length; ++i) {
        window.CreateCkEditor(editors[i], editor => {
            editor.editing.view.document.on('blur', () => {
                var textarea = editors[i];
                setFieldDpiaResponse(textarea.dataset.questionid, "response", editor.getData());
            });
        });
    }

    let conclusionEditor = document.getElementById('conclusion');
    window.CreateCkEditor(conclusionEditor, editor => {
        editor.editing.view.document.on('blur', () => {
            setFieldDpia("conclusion", editor.getData());
        });
    });

    // Bind foldable section events after content is loaded
    const sectionRows = document.querySelectorAll('.dpiaSchemaSectionTr');
    for (let i = 0; i < sectionRows.length; i++) {
        var row = sectionRows[i];
        var sectionId = row.dataset.sectionid;
        handleSectionRow(sectionId);
        row.addEventListener('click', sectionRowClicked, false);
    }

    const openedSection = sessionStorage.getItem(`openedDPIASectionId${assetId}`);
    if (openedSection !== null && openedSection !== undefined) {
        handleSectionRow(openedSection);
    }

    // Bind change event for checkboxes
    const checkboxes = document.querySelectorAll('input[type="checkbox"][id^="sectionCheckbox"]');
    for (let i = 0; i < checkboxes.length; i++) {
        checkboxes[i].addEventListener('click', (event) => event.stopPropagation()); // do not fold out/in section
        checkboxes[i].addEventListener('change', checkboxChanged);
    }

    const selectThreatAssessmentCheckboxes = document.querySelectorAll('.selectThreatAssessment');
    for (let i = 0; i < selectThreatAssessmentCheckboxes.length; i++) {
        selectThreatAssessmentCheckboxes[i].addEventListener('change', handleSelectThreatAssessmentCheckboxes);
    }

    // init send to select
    let responsibleSelect = document.getElementById('sendReportTo');
    if(responsibleSelect !== null) {
        initUserSelect('sendReportTo');
    }
}
