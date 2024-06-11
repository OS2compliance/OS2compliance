
var token = document.getElementsByName("_csrf")[0].getAttribute("content");

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

function setStyleNotRelevant(selected, rowId, rowClassName) {
    const selectAndTextareaElements = findSelectAndTextareaElements(rowClassName);
    let rows = document.querySelectorAll('.rowId' + rowId);
    // if not relevant
    if (selected === 'true') {
        rows.forEach(r => r.style.backgroundColor = "whitesmoke");
        disableOrEnableFields(selectAndTextareaElements, true)

        // reset numbers
        var selectElements = [];
        rows.forEach(r => findNumberSelects(r, selectElements));
        for (var i = 0; i < selectElements.length; i++) {
                var elem = selectElements[i];
                elem.value = -1;
        }
        var rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
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
    var rowId = this.dataset.rowid;
    var row = document.getElementById('row' + rowId);
    var rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
    calculateRisk(row, rowId, rowRiskScore);
    updateAverage();
}

function methodSelectChanged() {
    var value = this.value;
    var rowId = this.dataset.rowid;
    var residualRiskProbabilitySelect = document.getElementById('residualRiskProbabilityBtn' + rowId);
    var residualRiskConsequenceSelect = document.getElementById('residualRiskConsequenceBtn' + rowId);
    var rowResidualRiskScore = document.getElementById('row' + rowId + 'ResidualRiskScore');

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
    var rowId = elem.dataset.rowid;
    var rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
    calculateRisk(elem, rowId, rowRiskScore);
}

function calculateRisk(row, rowId, rowRiskScore) {
    var selectElements = [];
    findNumberSelects(row, selectElements);

    var probability = 0;
    var highestScore = 0;
    for (var i = 0; i < selectElements.length; i++) {
        var elem = selectElements[i];
        if (elem.classList.contains("probabilitySelect")) {
            if (elem.value > 0) {
                probability = elem.value;
            }
        } else {
            var number = elem.value;
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
    for (var i = 0; i < element.children.length; i++) {
        var child = element.children[i];

        if (child.tagName === "SELECT" && child.classList.contains("rowNumbers")) {
            selectElements.push(child);
        }

        findNumberSelects(child, selectElements);
    }
}

function setField() {
    var setFieldType = this.dataset.setfieldtype;
    var dbType = this.dataset.dbtype;
    var id = this.dataset.id;
    var identifier = this.dataset.identifier;
    var value = this.value;
    var data = {
                 "setFieldType": setFieldType,
                 "dbType": dbType,
                 "id": id,
                 "identifier": identifier,
                 "value": value
               };

    postData("/rest/risks/" + riskId + "/threats/setfield", data).then((response) => {
            if (!response.ok) {
                throw new Error(`${response.status} ${response.statusText}`);
            }
            toastService.info("Info", "Dine ændringer er blevet gemt")
        }).catch(error => {toastService.error("Der er sket en fejl og ændringerne kan ikke gemmes, genindlæs siden og prøv igen"); console.log(error)});
}

function updateAverage() {

    // probability
    var probabilities = document.querySelectorAll('.probabilities');
    var averageProbability = document.getElementById('averageProbability');
    calculateAverageForType(probabilities, averageProbability);

    // rfs
    var rfs = document.querySelectorAll('.rfs');
    var averageRF = document.getElementById('averageRF');
    calculateAverageForType(rfs, averageRF);

    // ris
    var ris = document.querySelectorAll('.ris');
    var averageRI = document.getElementById('averageRI');
    calculateAverageForType(ris, averageRI);

    // rts
    var rts = document.querySelectorAll('.rts');
    var averageRT = document.getElementById('averageRT');
    calculateAverageForType(rts, averageRT);

    // ofs
    var ofs = document.querySelectorAll('.ofs');
    var averageOF = document.getElementById('averageOF');
    calculateAverageForType(ofs, averageOF);

    // ois
    var ois = document.querySelectorAll('.ois');
    var averageOI = document.getElementById('averageOI');
    calculateAverageForType(ois, averageOI);

    // ots
    var ots = document.querySelectorAll('.ots');
    var averageOT = document.getElementById('averageOT');
    calculateAverageForType(ots, averageOT);

    // riskScores
    var riskScores = document.querySelectorAll('.riskScores');
    var averageRiskScore = document.getElementById('averageRiskScore');
    calculateAverageForRiskScore(riskScores, averageRiskScore);
}

function calculateAverageForType(selects, averageField) {
    if (selects == null || averageField == null) {
        return;
    }

    var numbers = [];
    for (var i = 0; i < selects.length; i++) {
        var elem = selects[i];
        var value = parseInt(elem.value);
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

    var numbers = [];
    for (var i = 0; i < fields.length; i++) {
        var elem = fields[i];
        var value = parseInt(elem.textContent);
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
  var sum = 0;
  for (var number of arr) {
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
    var value = this.value;
    var rowId = this.dataset.rowid;
    updateResidualRiskUIValue(value, this, rowId);
}

function updateResidualRiskUIValue(value, elem, rowId) {
    var residualRiskProbabilitySelect = document.getElementById('residualRiskProbabilityBtn' + rowId);
    var residualRiskConsequenceSelect = document.getElementById('residualRiskConsequenceBtn' + rowId);
    var rowResidualRiskScore = document.getElementById('row' + rowId + 'ResidualRiskScore');
    calculateResidualRiskScore(residualRiskProbabilitySelect, residualRiskConsequenceSelect, rowResidualRiskScore)
}

function calculateResidualRiskScore(residualRiskProbabilitySelect, residualRiskConsequenceSelect, rowResidualRiskScore) {
    if (residualRiskProbabilitySelect.value == '-1' || residualRiskConsequenceSelect.value == '-1') {
        rowResidualRiskScore.textContent = '';
        updateColorFor(rowResidualRiskScore, null);
        return;
    }

    var probabilityValue = parseInt(residualRiskProbabilitySelect.value);
    var consequenceValue = parseInt(residualRiskConsequenceSelect.value);
    rowResidualRiskScore.textContent = probabilityValue * consequenceValue;
    updateColorFor(rowResidualRiskScore, riskScoreColorMap[consequenceValue + "," + probabilityValue])
}

function updateColorFor(elem, color) {
    elem.style.backgroundColor = color;
    elem.style.color = foregroundColorForHex(color);
}

function categoryRowClicked() {
    var rowIndex = this.dataset.index;
    sessionStorage.setItem(`openedRowIndex${riskId}`, rowIndex);
    handleCategoryRow(rowIndex);
}

function handleCategoryRow(rowIndex) {
    // hide and show belonging rows
    var show = false;
    var icon = document.getElementById("categoryIcon" + rowIndex);
    const belongingRows = document.querySelectorAll('.categoryRow' + rowIndex);
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

    const relatedTasksRowsToShow = document.querySelectorAll('.relatedTasksRow' + rowIndex);
    if (show) {
        for (var i = 0; i < relatedTasksRowsToShow.length; i++) {
            relatedTasksRowsToShow[i].hidden = false;
        }
    } else {
        for (var i = 0; i < relatedTasksRowsToShow.length; i++) {
            relatedTasksRowsToShow[i].hidden = true;
        }
    }
}

function mailReportToRelatedOwner(assessmentId) {
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");
    fetch( `/rest/risks/${assessmentId}/mailReport`,
        {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token,
            }
        })
        .then(response => {
            if (response.status  > 299) {
                response.json()
                    .then(json => toastService.error(json.error));
            } else {
                toastService.info("Sendt");
            }
        })
        .catch(error => {
            toastService.error(error);
        });
}

function createTaskClicked(elem) {
    // Find the category row
    let row = elem.closest('.threatRow');
    var rowIndex = row.dataset.index;
    sessionStorage.setItem(`openedRowIndex${riskId}`, rowIndex);
    createTaskService.show(elem);
}

function deleteThreatClicked(elem) {
    console.log(`delete ${elem.dataset.customid} in assessment ${elem.dataset.riskid}`)
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
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}

function setPrecautions() {
    var dbType = this.dataset.dbtype;
    var threatId = this.dataset.id;
    var threatIdentifier = this.dataset.identifier;
    const selected = this.querySelectorAll('option:checked');
    const precautionIds = Array.from(selected).map(el => el.value);

    var data = {
                 "threatType": dbType,
                 "threatId": threatId,
                 "threatIdentifier": threatIdentifier,
                 "precautionIds": precautionIds
               };

    postData("/rest/risks/" + riskId + "/threats/setPrecautions", data).then((response) => {
            if (!response.ok) {
                throw new Error(`${response.status} ${response.statusText}`);
            }
            toastService.info("Info", "Dine ændringer er blevet gemt")
        }).catch(error => {toastService.error("Der er sket en fejl og ændringerne kan ikke gemmes, genindlæs siden og prøv igen"); console.log(error)});
}

function pageLoaded() {
    initFormValidationForForm("createCustomThreatModal");

    const excelTextareas = document.querySelectorAll('.excel-textarea');
    for (var i = 0; i < excelTextareas.length; i++) {
        excelTextareas[i].addEventListener('input', autoAdjustTextareaHeight, false);
        autoAdjustTextareaHeightInit(excelTextareas[i]);
    }

    const residualRisks = document.querySelectorAll('.residualRisks');
        for (var i = 0; i < residualRisks.length; i++) {
            var elem = residualRisks[i];
            var residualRisk = elem.value;
            var rowId = elem.dataset.rowid;

            updateResidualRiskUIValue(residualRisk, elem, rowId);

            elem.addEventListener('change', updatedResidualRiskValue, false);
        }

    const notRelevantSelects = document.querySelectorAll('.notRelevantSelect');
    for (var i = 0; i < notRelevantSelects.length; i++) {
        notRelevantSelects[i].addEventListener('change', notRelevantSelectChanged, false);
        notRelevantSelectInit(notRelevantSelects[i]);
    }

    const numberSelects = document.querySelectorAll('.rowNumbers');
    for (var i = 0; i < numberSelects.length; i++) {
        numberSelects[i].addEventListener('change', numberSelectChanged, false);
    }

    const rows = document.querySelectorAll('.threatRow');
    for (var i = 0; i < rows.length; i++) {
        initCalculateRisk(rows[i]);
    }

    const setFieldFields = document.querySelectorAll('.setField');
    for (var i = 0; i < setFieldFields.length; i++) {
        setFieldFields[i].addEventListener('change', setField, false);
    }

    const methodSelects = document.querySelectorAll('.methodSelect');
    for (var i = 0; i < methodSelects.length; i++) {
        methodSelects[i].addEventListener('change', methodSelectChanged, false);
    }

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
    for (var i = 0; i < precautionChoiceSelects.length; i++) {
        const relationsSelect = precautionChoiceSelects[i];

        // threat data
        var dbType = relationsSelect.dataset.dbtype;
        var id = relationsSelect.dataset.id;
        var identifier = relationsSelect.dataset.identifier;

        let relationsChoice = initSelect(relationsSelect);
        updateRelatedPrecautions(relationsChoice, "", dbType, id, identifier);
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
}
