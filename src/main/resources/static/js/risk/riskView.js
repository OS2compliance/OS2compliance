function notRelevantSelectChanged() {
    var selected = this.value;
    var rowId = this.dataset.rowid;
    var row = document.getElementById('row' + rowId);
    setStyleNotRelevant(selected, rowId, row);
    updateAverage();
}

function notRelevantSelectInit(elem) {
    var selected = elem.value;
    var rowId = elem.dataset.rowid;
    var row = document.getElementById('row' + rowId);
    setStyleNotRelevant(selected, rowId, row);
}

function setStyleNotRelevant(selected, rowId, row) {
    var selectAndTextareaElements = [];
    findSelectAndTextareaElements(row, selectAndTextareaElements);

    // if not relevant
    if (selected == 'true') {
        row.style.backgroundColor = "whitesmoke";
        disableOrEnableFields(selectAndTextareaElements, true)

        // reset numbers
        var selectElements = [];
        findNumberSelects(row, selectElements);
        for (var i = 0; i < selectElements.length; i++) {
                var elem = selectElements[i];
                elem.value = -1;
        }
        var rowRiskScore = document.getElementById('row' + rowId + 'RiskScore');
        rowRiskScore.textContent = "";
        updateColorFor(rowRiskScore, "INGEN")
    } else {
        row.style.backgroundColor = "white";
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
            element.style.backgroundColor = "white";
        }
    });
}

function findSelectAndTextareaElements(element, selectAndTextareaElements) {
    for (var i = 0; i < element.children.length; i++) {
        var child = element.children[i];

        if (child.tagName === "SELECT" || child.tagName === "TEXTAREA") {
            selectAndTextareaElements.push(child);
        }

        findSelectAndTextareaElements(child, selectAndTextareaElements);
    }
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
        updateColorFor(rowRiskScore, "INGEN");

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

    postData("/rest/risks/" + riskId + "/threats/setfield", data).then((data) => {
      // TODO måske vis ok eller fejl notifikation som toastr.js fx via data.status
    });
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
        updateColorFor(rowResidualRiskScore, "INGEN");
        return;
    }

    var probabilityValue = parseInt(residualRiskProbabilitySelect.value);
    var consequenceValue = parseInt(residualRiskConsequenceSelect.value);
    rowResidualRiskScore.textContent = probabilityValue * consequenceValue;
    updateColorFor(rowResidualRiskScore, riskScoreColorMap[consequenceValue + "," + probabilityValue])
}

function updateColorFor(elem, color) {
    // the color codes is the colors used for btns
    if (color === "GRØN") {
        elem.style.backgroundColor = "#87ad27";
        elem.style.color = "white";
    } else if (color === "GUL") {
        elem.style.backgroundColor = "#FFDE07FF";
        elem.style.color = "black";
    } else if (color === "RØD") {
        elem.style.backgroundColor = "#df5645";
        elem.style.color = "white";
    } else {
        elem.style.backgroundColor = "";
        elem.style.color = "#75868f";
    }
}

function pageLoaded() {
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
}
