function AssetDpiaService() {
    //    this.dpiaViewElem = null;
    //    this.dpiaOptOutViewElem = null;
    //    this.tabBadgeElem = null;
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
        //        this.dpiaViewElem = document.getElementById('dpiaView');
        //        this.dpiaOptOutViewElem = document.getElementById('dpiaOptOutTextView');
        //        this.tabBadgeElem = document.getElementById("dpiaBadge");
        this.screeningBadgeElem = document.getElementById("dpiaBadge2");
        this.recommendationElem = document.getElementById("recommendation");
        this.recommendationCardElem = document.getElementById("recommendationCard");
        this.handleAnswerChange();
        //        this.setDpiaVisibility(!asset.dpiaOptOut);
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

    //    this.setDpiaVisibility = function (visible) {
    //        this.dpiaViewElem.style.display = visible ? 'block' : 'none';
    //        this.dpiaOptOutViewElem.style.display = visible ? 'none' : 'block';
    //    }

    this.updateDpiaBadges = function () {
        if (asset.dpiaOptOut) {
            if (this.tabBadgeElem) {
                this.tabBadgeElem.classList.add("bg-gray-800");
            }
            this.screeningBadgeElem.classList.add("bg-gray-800");
            return;
        }
        if (this.tabBadgeElem) {
            this.tabBadgeElem.classList.value = 'badge';
        }
        this.screeningBadgeElem.classList.value = 'badge';
        if (this.screeningRed) {
            if (this.tabBadgeElem) {
                this.tabBadgeElem.classList.add("bg-red");
            }
            this.screeningBadgeElem.classList.add("bg-red");
        } else if (this.screeningYellow) {
            if (this.tabBadgeElem) {
                this.tabBadgeElem.classList.add("bg-yellow");
            }
            this.screeningBadgeElem.classList.add("bg-yellow");
        } else if (this.screeeningAnsweredCount > 0) {
            if (this.tabBadgeElem) {
                this.tabBadgeElem.classList.add("bg-green");
            }
            this.screeningBadgeElem.classList.add("bg-green");
        } else {
            if (this.tabBadgeElem) {
                this.tabBadgeElem.classList.add("bg-gray-800");
            }
            this.screeningBadgeElem.classList.add("bg-gray-800");
        }
    };

    //    this.setDpiOptOut = function (checkboxElem) {
    //        let optOut = checkboxElem.checked;
    //        asset.dpiaOptOut = optOut;
    //        let optOutBoolean = optOut ? 'true' : 'false';
    //        assetDetailsService.setField('dpiaOptOut', optOutBoolean);
    //        this.updateDpiaBadges();
    //        this.setDpiaVisibility(!optOut);
    //    }

    //    this.updateDpiaOptOutReason = function(elem) {
    //        assetDetailsService.setField('dpiaOptOutReason', elem.value);
    //    }

    this.editConsequenceLink = function() {
        document.getElementById('consequenceLinkSaveBtn').style.display = 'block';
        document.getElementById('consequenceLinkInput').style.display = 'block';
        document.getElementById('consequenceLinkEditBtn').style.display = 'none';
        document.getElementById('consequenceLink').style.display = 'none';
    }

    this.saveConsequenceLink = function() {
        let linkInput = document.getElementById('consequenceLinkInput');
        let linkTag = document.getElementById('consequenceLink');

        linkTag.text = linkInput.value;

        document.getElementById('consequenceLinkSaveBtn').style.display = 'none';
        linkInput.style.display = 'none';
        document.getElementById('consequenceLinkEditBtn').style.display = 'inline-block';
        linkTag.style.display = 'inline-block';

        this.setFieldScreening("consequenceLink", linkInput.value)
    }

    this.setFieldScreening = function (fieldName, value) {
        putData(`/rest/assets/${assetId}/dpiascreening/setfield?name=${fieldName}&value=${value}`)
        .then(defaultResponseHandler)
        .catch(defaultErrorHandler)
    }

    this.handleAnswerChange = function (event) {
        this.saveScreeningChanges(event)
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

    this.setRevisionInterval = function(assetId) {
        fetch( `/assets/${assetId}/revision`)
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

    this.saveScreeningChanges = async (event)=> {
        if (!event) {return;}
        const target = event.target
        const choiceIdentifier = target.id
        const selectedOption = target.selectedOptions[0]
        const data = {
            "assetId": assetId,
            "answer": selectedOption.value,
            "choiceIdentifier": choiceIdentifier
        }

        const url = `${restUrl}/screening/update`
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

        toastService.info("Valg gemt")
    }

}

function setDPIAEditState(enabled) {
    const rootElement = document.getElementById('dpiaRoot');
    if (enabled) {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = false;
            if (elem.tagName == "A") {
                elem.hidden = true;
                if (elem.nextElementSibling) {
                    elem.nextElementSibling.hidden = false;
                }
            }

            if (elem.classList.contains("datepicker")) {
                elem.parentElement.hidden = false; // show the datepicker
                if (elem.parentElement.nextElementSibling) {
                    elem.parentElement.nextElementSibling.hidden = true; // hide the "ingen" textfield
                }
            }
        });

        rootElement.querySelectorAll('.qualityCheck').forEach(elem => {
            elem.disabled = false;
        });

        //                document.getElementById('saveDPIABtn').hidden = false;
        //                document.getElementById('editDPIABtn').hidden = true;
        //                document.getElementById('cancelDPIABtn').hidden = false;
    } else {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = true;
            if (elem.tagName == "A") {
                elem.hidden = false;
                elem.nextElementSibling.hidden = true;
            }

            if (elem.classList.contains("datepicker")) {
                if (elem.value == null || elem.value == "") {
                    elem.parentElement.hidden = true; // hide the datepicker
                    elem.parentElement.nextElementSibling.hidden = false; // show the "ingen" textfield
                }
            }
        });

        rootElement.querySelectorAll('.qualityCheck').forEach(elem => {
            elem.disabled = true;
        });

        //               document.getElementById('saveDPIABtn').hidden = true;
        //               document.getElementById('editDPIABtn').hidden = false;
        //              document.getElementById('cancelDPIABtn').hidden = true;

        const form = document.getElementById("dpiaForm");
        form.reset();
        assetDpiaService.handleAnswerChange();
    }
}

