function setColors() {
    var fields = document.querySelectorAll('.riskScoreField');
    fields.forEach(function(field, index) {
        var consequence = field.dataset.consequence;
        var probability = field.dataset.probability;
        var color = scaleColorMap[consequence + "," + probability];
        updateColorFor(field, color);
    });

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

function setBeforeNumbers() {
    riskProfiles.forEach(function(riskProfile, index) {
        var matchingField = document.getElementById('before' + riskProfile.consequence + ',' + riskProfile.probability);
        if (matchingField.textContent.trim() === "") {
            matchingField.textContent = (riskProfile.index + 1);
        } else {
            matchingField.textContent = matchingField.textContent + ', ' + (riskProfile.index+1);
        }
    });
}

function setAfterNumbers() {
    riskProfiles.forEach(function(riskProfile, index) {
        var residualValues = false;
        var consequence = riskProfile.consequence;
        var probability = riskProfile.probability;
        if (riskProfile.residualConsequence !== -1 && riskProfile.residualProbability !== -1) {
            consequence = riskProfile.residualConsequence;
            probability = riskProfile.residualProbability;
            residualValues = true;
        }

        var matchingField = document.getElementById('after' + consequence + ',' + probability);
        if (matchingField.textContent.trim() === "") {
            matchingField.textContent = (riskProfile.index+1) + (residualValues ? '*' : '');
        } else {
            matchingField.textContent = matchingField.textContent + ', ' + (riskProfile.index+1) + (residualValues ? '*' : '');
        }
    });
}

function profilePageLoaded() {
    setColors();
    setBeforeNumbers();
    setAfterNumbers();

}
