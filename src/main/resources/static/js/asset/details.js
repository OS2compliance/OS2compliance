
let assetDetailsService = new AssetDetailsService();

document.addEventListener("DOMContentLoaded", function (event) {
    assetDetailsService.init();
});

// Required variables asset & assessment
function AssetDetailsService() {

    this.init = function() {
        this.updateRiskAssessmentBadge();
        this.setTreatAssessmentVisibility(!asset.threatAssessmentOptOut);
    }

    this.setField = function (fieldName, value) {
        putData(`/rest/assets/${assetId}/setfield?name=${fieldName}&value=${value}`)
            .then(defaultResponseHandler)
            .catch(defaultErrorHandler)
    }

    this.setTreatAssessmentVisibility = function(show) {
        document.getElementById('threatAssessmentView').style.display = show ? 'block' : 'none';
        document.getElementById('threatAssessmentOptOutView').style.display = show ? 'none' : 'block';
    }

    this.updateThreatAssessmentOptOutReason = function(elem) {
        this.setField('threatAssessmentOptOutReason', elem.value);
    }

    this.updateRiskAssessmentBadge = function() {
        let badgeElem = document.getElementById('riskAssessmentBadge');
        badgeElem.classList.value = ''
        ensureElementHasClass(badgeElem, 'badge');
        if (asset.threatAssessmentOptOut === true) {
            ensureElementHasClass(badgeElem, 'bg-gray-800');
        } else if ('RED' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-red');
        } else if ('YELLOW' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-yellow');
        } else if ('GREEN' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-green');
        } else {
            ensureElementHasClass(badgeElem, 'bg-gray-800');
        }
    }

    this.setRiskAssessmentOptOut = function(checkboxElem) {
        let optOut = checkboxElem.checked;
        asset.threatAssessmentOptOut = optOut;
        let optOutBoolean = optOut ? 'true' : 'false';
        this.setField('threatAssessmentOptOut', optOutBoolean);
        this.updateRiskAssessmentBadge();
        this.setTreatAssessmentVisibility(!optOut);
    }

}
