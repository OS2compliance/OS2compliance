
let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", async function(event) {
    pageLoaded();
    const { default: initRelatedTagList } = await import("../tags/related-tag-list.js");
    initRelatedTagList();
});

document.addEventListener("DOMContentLoaded", function() {
    document.addEventListener("click", function(event) {
        if (event.target.classList.contains("edit-threat-button")) {
            editThreatClicked(event.target);
        }
    });

    document.addEventListener("click", function(event) {
        if (event.target.classList.contains("task-button")) {
            createTaskClicked(event.target);
        }
    });

    document.addEventListener("click", function(event) {
        if (event.target.classList.contains("delete-threat-button")) {
            deleteThreatClicked(event.target);
        }
    });

    document.addEventListener("click", function(event) {
        if (event.target.classList.contains("form-reset-button")) {
            formReset();
        }
    })
})

function notRelevantSelectChanged() {
    const selected = this.value;
    const rowId = this.dataset.rowid;
    setStyleNotRelevant(selected, rowId, 'rowId' + rowId);
    updateAverage();
}

function notRelevantSelectInit(elem) {
    const selected = elem.value;
    const rowId = elem.dataset.rowid;
    setStyleNotRelevant(selected, rowId, 'rowId' + rowId);
}

this.initCommentField = ()=> {
    const commentFieldElement = document.getElementById('riskCommentArea')
    commentFieldElement.addEventListener('change', async (event)=> {
        if (commentFieldElement !== null) {
            this.onCommentEdit(commentFieldElement.value)
        }
    })
}

this.onCommentEdit = async (commentValue) => {

    const data = {
        riskId: riskId,
        comment: commentValue
    }
    const url = `/rest/risks/comment/update`
    const response = await fetch(url, {
        method: "POST",
        headers: {
            'X-CSRF-TOKEN': token,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(data)
    })

    if (!response.ok)  {
        toastService.error(response.statusText)
    }

    toastService.info("Kommentar gemt")
}

function setStyleNotRelevant(selected, rowId, rowClassName) {
    const selectAndTextareaElements = findSelectAndTextareaElements(rowClassName);
    let rows = document.querySelectorAll('.rowId' + rowId);
    // if not relevant
    if (selected === 'true') {
        rows.forEach(r => r.style.backgroundColor = "whitesmoke");
        disableOrEnableFields(selectAndTextareaElements, true)

        // reset numbers
        let selectElements = [];
        rows.forEach(r => findNumberSelects(r, selectElements));
        for (let i = 0; i < selectElements.length; i++) {
            let elem = selectElements[i];
                elem.value = -1;
        }
        let rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
        rowRiskScore.textContent = "";
        updateColorFor(rowRiskScore, null)
    } else {
        rows.forEach(r => r.style.backgroundColor = "transparent");
        disableOrEnableFields(selectAndTextareaElements, false)
    }
}

function disableOrEnableFields(selectAndTextareaElements, disable) {
    selectAndTextareaElements.forEach(function (element) {
        if (!element.classList.contains("notRelevantSelect")) {
            element.disabled = disable;
        }
        if (disable) {
            element.style.backgroundColor = "whitesmoke";
        } else {
            element.style.backgroundColor = "transparent";
        }
    });
}

function findSelectAndTextareaElements(categoryClassName) {
    const elements = document.querySelectorAll('.' + categoryClassName);
    let resultElements = [];
    elements.forEach(e => {
        e.querySelectorAll('select, textarea')
            .forEach(s => resultElements.push(s));
    });
    return resultElements;
}

function numberSelectChanged() {
    let rowId = this.dataset.rowid;
    let row = document.getElementById('row' + rowId);
    let rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
    calculateRisk(row, rowId, rowRiskScore);
    updateAverage();
}

function methodSelectChanged() {
    let value = this.value;
    let rowId = this.dataset.rowid;
    let residualRiskProbabilitySelect = document.getElementById('residualRiskProbabilityBtn' + rowId);
    let residualRiskConsequenceSelect = document.getElementById('residualRiskConsequenceBtn' + rowId);
    let rowResidualRiskScore = document.getElementById('row' + rowId + 'ResidualRiskScore');

    if (value == 'ACCEPT' || value == 'NONE') {
        residualRiskProbabilitySelect.style.display = 'none';
        residualRiskConsequenceSelect.style.display = 'none';
        rowResidualRiskScore.textContent = '';
    } else {
        residualRiskProbabilitySelect.style.display = '';
        residualRiskConsequenceSelect.style.display = '';
        calculateResidualRiskScore(residualRiskProbabilitySelect, residualRiskConsequenceSelect, rowResidualRiskScore);
    }
}

function initCalculateRisk(elem) {
    let rowId = elem.dataset.rowid;
    let rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
    calculateRisk(elem, rowId, rowRiskScore);
}

function calculateRisk(row, rowId, rowRiskScore) {
    let selectElements = [];
    findNumberSelects(row, selectElements);

    let probability = 0;
    let highestScore = 0;
    for (let i = 0; i < selectElements.length; i++) {
        let elem = selectElements[i];
        if (elem.classList.contains("probabilitySelect")) {
            if (elem.value > 0) {
                probability = elem.value;
            }
        } else {
            let number = elem.value;
            if (number > highestScore) {
                highestScore = number;
            }
        }
    }

    if (probability == 0 || highestScore == 0) {
        rowRiskScore.textContent = "";
        updateColorFor(rowRiskScore, null);

    } else {
        rowRiskScore.textContent = probability * highestScore;
        updateColorFor(rowRiskScore, riskScoreColorMap[highestScore + "," + probability]);
    }
}

function findNumberSelects(element, selectElements) {
    for (let i = 0; i < element.children.length; i++) {
        let child = element.children[i];

        if (child.tagName === "SELECT" && child.classList.contains("rowNumbers")) {
            selectElements.push(child);
        }

        findNumberSelects(child, selectElements);
    }
}

function setField() {
    let setFieldType = this.dataset.setfieldtype;
    let dbType = this.dataset.dbtype;
    let id = this.dataset.id;
    let identifier = this.dataset.identifier;
    let value = this.value;

    let validated = validateFieldBeforeSetting(setFieldType, value);
    if (!validated) {
        return;
    }

    let data = {
                 "setFieldType": setFieldType,
                 "dbType": dbType,
                 "id": id,
                 "identifier": identifier,
                 "value": value
               };

    postData("/rest/risks/" + riskId + "/threats/setfield", data).then((response) => {
            defaultResponseErrorHandler(response);
            toastService.info("Info", "Dine ændringer er blevet gemt")
        }).catch(error => {toastService.error("Der er sket en fejl og ændringerne kan ikke gemmes, genindlæs siden og prøv igen"); console.error(error)});
}

function validateFieldBeforeSetting(setFieldType, value) {
    if (setFieldType == 'PROBLEM') {
        if (value.length >= 2048) {
            toastService.error("Teksten i 'Problemstilling' må maksimalt være 2048 tegn")
            return false;
        }
    }
    return true;
}

function toggleAllCategories() {
    const allIcons = document.querySelectorAll('[id^="categoryIcon"]');

    let openCount = 0;
    let closedCount = 0;

    allIcons.forEach(icon => {
        if (icon.classList.contains('pli-arrow-down')) {
            closedCount++;
        } else {
            openCount++;
        }
    });

    const shouldClose = openCount >= closedCount;

    allIcons.forEach(icon => {
        const isOpen = icon.classList.contains('pli-arrow-up');

        if ((shouldClose && isOpen) || (!shouldClose && !isOpen)) {
            icon.click();
        }
    });

    const arrowTag = document.getElementById('arrowTag');
    if (arrowTag) {
        if (shouldClose) {
            arrowTag.classList.remove('pli-arrow-up');
            arrowTag.classList.add('pli-arrow-down');
        } else {
            arrowTag.classList.remove('pli-arrow-down');
            arrowTag.classList.add('pli-arrow-up');
        }
    }
}

function updateAverage() {

    // probability
    let probabilities = document.querySelectorAll('.probabilities');
    let averageProbability = document.getElementById('averageProbability');
    calculateAverageForType(probabilities, averageProbability);

    // rfs
    let rfs = document.querySelectorAll('.rfs');
    let averageRF = document.getElementById('averageRF');
    calculateAverageForType(rfs, averageRF);

    // ris
    let ris = document.querySelectorAll('.ris');
    let averageRI = document.getElementById('averageRI');
    calculateAverageForType(ris, averageRI);

    // rts
    let rts = document.querySelectorAll('.rts');
    let averageRT = document.getElementById('averageRT');
    calculateAverageForType(rts, averageRT);

    // ofs
    let ofs = document.querySelectorAll('.ofs');
    let averageOF = document.getElementById('averageOF');
    calculateAverageForType(ofs, averageOF);

    // ois
    let ois = document.querySelectorAll('.ois');
    let averageOI = document.getElementById('averageOI');
    calculateAverageForType(ois, averageOI);

    // ots
    let ots = document.querySelectorAll('.ots');
    let averageOT = document.getElementById('averageOT');
    calculateAverageForType(ots, averageOT);

    // sfs
    let sfs = document.querySelectorAll('.sfs');
    let averageSF = document.getElementById('averageSF');
    calculateAverageForType(sfs, averageSF);

    // sis
    let sis = document.querySelectorAll('.sis');
    let averageSI = document.getElementById('averageSI');
    calculateAverageForType(sis, averageSI);

    // sts
    let sts = document.querySelectorAll('.sts');
    let averageST = document.getElementById('averageST');
    calculateAverageForType(sts, averageST);

    // sas
    let sas = document.querySelectorAll('.sas');
    let averageSA = document.getElementById('averageSA');
    calculateAverageForType(sas, averageSA);

    // riskScores
    let riskScores = document.querySelectorAll('.riskScores');
    let averageRiskScore = document.getElementById('averageRiskScore');
    calculateAverageForRiskScore(riskScores, averageRiskScore);
}

function calculateAverageForType(selects, averageField) {
    if (selects == null || averageField == null) {
        return;
    }

    let numbers = [];
    for (let i = 0; i < selects.length; i++) {
        let elem = selects[i];
        let value = parseInt(elem.value);
        if (value > 0) {
            numbers.push(value);
        }
    }

    if (numbers.length == 0) {
        averageField.textContent = 0;
    } else {
        averageField.textContent = average(numbers);
    }
}

function calculateAverageForRiskScore(fields, averageField) {
    if (fields == null || averageField == null) {
        return;
    }

    let numbers = [];
    for (let i = 0; i < fields.length; i++) {
        let elem = fields[i];
        let value = parseInt(elem.textContent);
        if (value > 0) {
            numbers.push(value);
        }
    }

    if (numbers.length == 0) {
        averageField.textContent = 0;
    } else {
        averageField.textContent = average(numbers);
    }
}

function average(arr) {
    let sum = 0;
  for (let number of arr) {
      sum += number;
  }
  return round(sum / arr.length);
}

function round(number) {
    return Math.round(number * 10) / 10;
}

function autoAdjustTextareaHeight() {
    this.style.height = "auto";
    this.style.height = (this.scrollHeight) + "px";
}

function autoAdjustTextareaHeightInit(textarea) {
    textarea.style.height = "auto";
    textarea.style.height = (textarea.scrollHeight) + "px";
}

function updatedResidualRiskValue() {
    let value = this.value;
    let rowId = this.dataset.rowid;
    updateResidualRiskUIValue(value, this, rowId);
}

function updateResidualRiskUIValue(value, elem, rowId) {
    let residualRiskProbabilitySelect = document.getElementById('residualRiskProbabilityBtn' + rowId);
    let residualRiskConsequenceSelect = document.getElementById('residualRiskConsequenceBtn' + rowId);
    let rowResidualRiskScore = document.getElementById('row' + rowId + 'ResidualRiskScore');
    calculateResidualRiskScore(residualRiskProbabilitySelect, residualRiskConsequenceSelect, rowResidualRiskScore)
}

function calculateResidualRiskScore(residualRiskProbabilitySelect, residualRiskConsequenceSelect, rowResidualRiskScore) {
    if (residualRiskProbabilitySelect.value == '-1' || residualRiskConsequenceSelect.value == '-1') {
        rowResidualRiskScore.textContent = '';
        updateColorFor(rowResidualRiskScore, null);
        return;
    }

    let probabilityValue = parseInt(residualRiskProbabilitySelect.value);
    let consequenceValue = parseInt(residualRiskConsequenceSelect.value);
    rowResidualRiskScore.textContent = probabilityValue * consequenceValue;
    updateColorFor(rowResidualRiskScore, riskScoreColorMap[consequenceValue + "," + probabilityValue])
}

function updateColorFor(elem, color) {
    elem.style.backgroundColor = color;
    elem.style.color = foregroundColorForHex(color);
}

function categoryRowClicked() {
    let rowIndex = this.dataset.index;
    sessionStorage.setItem(`openedRowIndex${riskId}`, rowIndex);
    handleCategoryRow(rowIndex);
}

function handleCategoryRow(rowIndex) {
    // hide and show belonging rows
    let show = false;
    let icon = document.getElementById("categoryIcon" + rowIndex);
    const belongingRows = document.querySelectorAll('.categoryRow' + rowIndex);
    for (let i = 0; i < belongingRows.length; i++) {
        let elem = belongingRows[i];
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

    const relatedTasksRowsToShow = document.querySelectorAll('.relatedTasksRow' + rowIndex);
    if (show) {
        for (let i = 0; i < relatedTasksRowsToShow.length; i++) {
            relatedTasksRowsToShow[i].hidden = false;
        }
    } else {
        for (let i = 0; i < relatedTasksRowsToShow.length; i++) {
            relatedTasksRowsToShow[i].hidden = true;
        }
    }
}

function mailReport() {
    let sendReportTo = document.getElementById('sendReportTo').value;
    let reportMessage = document.getElementById('reportMessage').value;
    let reportFormat = document.getElementById('reportFormat').value;
    let signReport = document.getElementById('signReport').checked;
    let alsoSendTo = document.getElementById('alsoSendTo');
    let selectedValues = [...alsoSendTo.selectedOptions].map(option => option.value);
    let data = {
                 "sendTo": sendReportTo,
                 "message": reportMessage,
                 "format": reportFormat,
                 "sign": signReport,
                 "alsoSendTo": selectedValues
               };

    postData(`/rest/risks/${riskId}/mailReport`, data).then((response) => {
        defaultResponseErrorHandler(response);
        toastService.info("Sendt");
        document.querySelector('#sendReportModal .btn-close').click();
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    }).catch(error => {toastService.error(error)});
}

function createTaskClicked(elem) {
    // Find the category row
    let row = elem.closest('.threatRow');
    let rowIndex = row.dataset.index;
    sessionStorage.setItem(`openedRowIndex${riskId}`, rowIndex);
    createTaskService.show(elem);
}

function deleteThreatClicked(elem) {
    Swal.fire({
        text: `Er du sikker på du vil slette denne trusslen?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/rest/risks/${elem.dataset.riskid}/threats/${elem.dataset.customid}`,
                {method: "DELETE", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                .then(response => location.reload())
                .catch(error => toastService.error(error));
        }
    });
}

function editThreatClicked(elem) {
    document.getElementById('customThreatId').value = elem.dataset.customid;
    document.getElementById('threatType').value = elem.dataset.type;
    document.getElementById('threatDescription').value = elem.dataset.description;

    editDialog = new bootstrap.Modal(document.getElementById('editCustomThreatFormDialog'));
    editDialog.show();
}

let revisionDialog;
function setRevisionInterval(assessmentId) {
    fetch( `/risks/${assessmentId}/revision`)
        .then(response => response.text()
            .then(data => {
                let dialog = document.getElementById('revisionIntervalDialog');
                dialog.innerHTML = data;
                revisionDialog = new bootstrap.Modal(document.getElementById('revisionIntervalDialog'));
                revisionDialog.show();
                initDatepicker("#nextRevisionBtn", "#nextRevision");
            }))
        .catch(error => toastService.error(error));
}

function updateRelatedPrecautions(choices, search, threatType, threatId, threatIdentifier) {
    fetch( `/rest/relatable/autocomplete/relatedprecautions?search=${search}&threatType=${threatType}&threatIdentifier=${threatIdentifier}&threatId=${threatId}&riskId=${riskId}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        // name: truncateString(reg.name + ": " + reg.description, 60),
                        name: reg.name + ": " + reg.description,
                        title: reg.description,
                        customProperties : {

                        }
                    }
                }), 'id', 'name', true);


            }))
        .catch(error => toastService.error(error));
}

function setPrecautions() {
    let dbType = this.dataset.dbtype;
    let threatId = this.dataset.id;
    let threatIdentifier = this.dataset.identifier;
    const selected = this.querySelectorAll('option:checked');
    const precautionIds = Array.from(selected).map(el => el.value);

    let data = {
                 "threatType": dbType,
                 "threatId": threatId,
                 "threatIdentifier": threatIdentifier,
                 "precautionIds": precautionIds
               };

    postData("/rest/risks/" + riskId + "/threats/setPrecautions", data).then((response) => {
            defaultResponseErrorHandler(response);
            toastService.info("Info", "Dine ændringer er blevet gemt")
        }).catch(error => {toastService.error("Der er sket en fejl og ændringerne kan ikke gemmes, genindlæs siden og prøv igen"); console.log(error)});
}

function pageLoaded() {
    initFormValidationForForm("createCustomThreatModal");

    const excelTextareas = document.querySelectorAll('.excel-textarea');
    for (let i = 0; i < excelTextareas.length; i++) {
        excelTextareas[i].addEventListener('input', autoAdjustTextareaHeight, false);
        autoAdjustTextareaHeightInit(excelTextareas[i]);
    }

    const residualRisks = document.querySelectorAll('.residualRisks');
        for (let i = 0; i < residualRisks.length; i++) {
            let elem = residualRisks[i];
            let residualRisk = elem.value;
            let rowId = elem.dataset.rowid;

            updateResidualRiskUIValue(residualRisk, elem, rowId);

            elem.addEventListener('change', updatedResidualRiskValue, false);
        }

    const notRelevantSelects = document.querySelectorAll('.notRelevantSelect');
    for (let i = 0; i < notRelevantSelects.length; i++) {
        notRelevantSelects[i].addEventListener('change', notRelevantSelectChanged, false);
        notRelevantSelectInit(notRelevantSelects[i]);
    }

    const numberSelects = document.querySelectorAll('.rowNumbers');
    for (let i = 0; i < numberSelects.length; i++) {
        numberSelects[i].addEventListener('change', numberSelectChanged, false);
    }

    const rows = document.querySelectorAll('.threatRow');
    for (let i = 0; i < rows.length; i++) {
        initCalculateRisk(rows[i]);
    }

    const setFieldFields = document.querySelectorAll('.setField');
    for (let i = 0; i < setFieldFields.length; i++) {
        setFieldFields[i].addEventListener('change', setField, false);
    }

    const methodSelects = document.querySelectorAll('.methodSelect');
    for (let i = 0; i < methodSelects.length; i++) {
        methodSelects[i].addEventListener('change', methodSelectChanged, false);
    }
    document.getElementById('toggleAllCategories').addEventListener('click', toggleAllCategories);

    updateAverage();

    // foldable categories
    const categoryRows = document.querySelectorAll('.categoryTr');
    for (let i = 0; i < categoryRows.length; i++) {
        handleCategoryRow(i);
        categoryRows[i].addEventListener('click', categoryRowClicked, false);
    }
    const openedRow = sessionStorage.getItem(`openedRowIndex${riskId}`);
    if (openedRow !== null && openedRow !== undefined) {
        handleCategoryRow(openedRow);
    }

    // precaution choice.js
    const precautionChoiceSelects = document.querySelectorAll('.select-precaution');
    for (let i = 0; i < precautionChoiceSelects.length; i++) {
        const relationsSelect = precautionChoiceSelects[i];

        // threat data
        let dbType = relationsSelect.dataset.dbtype;
        let id = relationsSelect.dataset.id;
        let identifier = relationsSelect.dataset.identifier;

        const initPrecautionSelect = (element, containerInner = 'form-control') => {
            let choices = new Choices(element, {
                searchChoices: false,
                removeItemButton: true,
                allowHTML: true,
                searchFloor: 0,
                searchPlaceholderValue: 'Søg...',
                searchResultLimit: 50,
                itemSelectText: 'Vælg',
                noChoicesText: 'Søg...',
                noResultsText: 'Ingen fundet',
                classNames: {
                    containerInner: containerInner
                },
                duplicateItemsAllowed: false,
                shouldSort: false,
            });
            element.addEventListener("change",
                function(event) {
                    choices.hideDropdown();
                },
                false,
            );
            element.addEventListener("showDropdown",
                function(event) {
                    updateRelatedPrecautions(relationsChoice, event.detail.value ? event.detail.value : "" , dbType, id, identifier);
                },
                false,
            );
            return choices;
        }
        let relationsChoice = initPrecautionSelect(relationsSelect, "excel-textarea");

        relationsSelect.addEventListener("search",
            function(event) {
                updateRelatedPrecautions(relationsChoice, event.detail.value, dbType, id, identifier);
            },
            false,
        );
        relationsSelect.addEventListener("change",
            function(event) {
                updateRelatedPrecautions(relationsChoice, "", dbType, id, identifier);
            },
            false,
        );

        // on change listener
        relationsSelect.addEventListener('change', setPrecautions, false);
    }

    // init send to select
    let responsibleSelect = document.getElementById('sendReportTo');
    if(responsibleSelect !== null) {
        choiceService.initUserSelect('sendReportTo');
    }
    // init send also to select
    let sendAlsoToSelect = document.getElementById('alsoSendTo');
    if(sendAlsoToSelect !== null) {
        choiceService.initUserSelect('alsoSendTo');
    }

    // checkbox listener
    let signReportCheckbox = document.getElementById('signReport');
    signReportCheckbox.addEventListener('change', function() {
        let formatSelect = document.getElementById('reportFormat');
        if (this.checked) {
            formatSelect.value = 'PDF';
            formatSelect.disabled = true;
        } else {
            formatSelect.disabled = false;
        }
    });

    // make page read only depending on report status
    if (threatAssessmentReportApprovalStatus != null && (threatAssessmentReportApprovalStatus == "WAITING" || threatAssessmentReportApprovalStatus == "SIGNED")) {
        document.querySelectorAll('input, textarea, select').forEach(function(element) {
            element.readOnly = true;
        });
        document.querySelectorAll('input, textarea, select, button').forEach(function(element) {
            element.disabled = true;
        });
        document.querySelectorAll('.disableIfReadonly').forEach(function(element) {
            element.onclick = function(event) {
                event.preventDefault();
            };
        });
        document.querySelectorAll('.showIfReadOnly').forEach(function(element) {
            element.style.display = 'block';
        });
        document.querySelectorAll('.hideIfReadOnly').forEach(function(element) {
            element.style.display = 'none';
        });
    }

    // init comment field
    initCommentField();

    // init threatCatalog modal
    const catalogSelect = document.getElementById('editThreatCatalogSelect');
    initSelectWithConfirmation(catalogSelect);

}
