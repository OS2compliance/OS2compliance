
let assetDetailsService = new AssetDetailsService();
let assetDpiaService = new AssetDpiaService();
document.addEventListener("DOMContentLoaded", function (event) {
    assetDetailsService.init();
    assetDpiaService.init();
});

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

function AssetDpiaService() {
    this.dpiaViewElem = null;
    this.tabBadgeElem = null;
    this.screeningBadgeElem = null;
    this.recommendationElem = null;
    this.recommendationCardElem = null;

    this.screeningStats = {};
    this.screeningCategoryStats = {};
    this.screeeningAnsweredCount = 0;
    this.screeningCategoriesFlagged = 0;
    this.screeningRed = false;
    this.screeningYellow = false;

    this.init = function() {
        this.dpiaViewElem = document.getElementById('dpiaView');
        this.tabBadgeElem = document.getElementById("dpiaBadge");
        this.screeningBadgeElem = document.getElementById("dpiaBadge2");
        this.recommendationElem = document.getElementById("recommendation");
        this.recommendationCardElem = document.getElementById("recommendationCard");
        this.handleAnswerChange();
        this.setDpiaVisibility(!asset.dpiaOptOut);
    }

    this.updateStatistics = function() {
        this.screeningStats = Array.from(document.querySelectorAll("select[id^='dpia-']"))
            .map(e => {
                return {
                    id: e.id,
                    value: this.getElementValue(e),
                    answered: e.value !== '',
                    category: e.dataset.category
                }
            });
        console.log(JSON.stringify(this.screeningStats))
        this.screeeningAnsweredCount = this.screeningStats.reduce((acc, e) => e.answered ? acc+1 : acc, 0);
        this.screeningCategoryStats = this.screeningStats.reduce((acc, e) => {
            acc[e.category] = !!acc[e.category] || e.value;
            return acc;
        }, {});
        this.screeningCategoriesFlagged = Object.entries(this.screeningCategoryStats).reduce((acc, e) => {
            return acc + (e[1] ? 1 : 0);
        }, 0);
        let alwaysRedAnswer = this.screeningStats.filter(e => e.id === 'dpia-7' && e.value).length > 0;
        this.screeningRed = this.screeningCategoriesFlagged >= 2 || alwaysRedAnswer;
        this.screeningYellow = !this.screeningRed && this.screeningCategoriesFlagged > 0;
    }

    this.setDpiaVisibility = function (visible) {
        this.dpiaViewElem.style.display = visible ? 'block' : 'none';
    }

    this.updateDpiaBadges = function () {
        if (asset.dpiaOptOut) {
            this.tabBadgeElem.classList.add("bg-gray-800");
            this.screeningBadgeElem.classList.add("bg-gray-800");
            return;
        }
        this.tabBadgeElem.classList.value = 'badge';
        this.screeningBadgeElem.classList.value = 'badge';
        if (this.screeningRed) {
            this.tabBadgeElem.classList.add("bg-red");
            this.screeningBadgeElem.classList.add("bg-red");
        } else if (this.screeningYellow) {
            this.tabBadgeElem.classList.add("bg-yellow");
            this.screeningBadgeElem.classList.add("bg-yellow");
        } else if (this.screeeningAnsweredCount > 0) {
            this.tabBadgeElem.classList.add("bg-green");
            this.screeningBadgeElem.classList.add("bg-green");
        } else {
            this.tabBadgeElem.classList.add("bg-gray-800");
            this.screeningBadgeElem.classList.add("bg-gray-800");
        }
    };

    this.setDpiOptOut = function (checkboxElem) {
        let optOut = checkboxElem.checked;
        asset.dpiaOptOut = optOut;
        let optOutBoolean = optOut ? 'true' : 'false';
        assetDetailsService.setField('dpiaOptOut', optOutBoolean);
        this.updateDpiaBadges();
        this.setDpiaVisibility(!optOut);
    }

    this.handleAnswerChange = function () {
        this.updateStatistics();
        let maxForYellowExceeded = this.screeningCategoriesFlagged >= 2;
        this.screeningStats.forEach(status => {
            this.setBadge(status, maxForYellowExceeded);
        });
        this.updateRecommendation();
        this.updateDpiaBadges();
    }

    this.updateRecommendation = function () {
        this.recommendationCardElem.classList.remove("border-danger","border-warning","border-light");
        if (this.screeningRed) {
            this.recommendationElem.textContent = "Du skal udføre konsekvensanalyse da behandlingen sandsynligvis indebærer en høj risiko for de registrerede (se røde bekymringer)";
            this.recommendationCardElem.classList.add("border-danger");
        } else if (this.screeningYellow) {
            this.recommendationElem.textContent = "Du skal ikke udføre konsekvensanalyse da behandlingen sandsynligvis indebærer risiko for de registrerede, men ikke en høj risiko (se gule bekymringer)";
            this.recommendationCardElem.classList.add("border-warning");
        } else {
            this.recommendationElem.textContent = "Du skal ikke udføre konsekvensanalyse da behandlingen sandsynligvis ikke indebærer risiko for de registrerede";
            this.recommendationCardElem.classList.add("border-light");
        }
    }

    this.getElementValue = function(element) {
        if (element.value) {
            return element.value === 'dpia-yes' || element.value === 'dpia-partially' || element.value === 'dpia-dont-know';
        } else {
            return false;
        }
    }

    this.setBadge = function(screeningStatus, maxForYellowExceeded) {
        let element = document.getElementById(screeningStatus.id);
        if (screeningStatus.value === false) {
            element.parentElement.nextElementSibling.innerHTML = '<div class="d-block mx-auto badge bg-gray-800">Blank</div>';
        } else {
            if (maxForYellowExceeded || screeningStatus.id === "dpia-7") {
                element.parentElement.nextElementSibling.innerHTML = '<div class="d-block mx-auto badge bg-danger">Rød</div>';
            } else {
                element.parentElement.nextElementSibling.innerHTML = '<div class="d-block mx-auto badge bg-warning">Gul</div>';
            }
        }
    }

}
