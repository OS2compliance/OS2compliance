
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
        } else if ('ORANGE' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-orange');
        } else if ('GREEN' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-green');
        } else if ('LIGHT_GREEN' === assessment) {
            ensureElementHasClass(badgeElem, 'bg-green-300');
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

    this.addProductLink = function() {
        const container = document.getElementById("productLinksEditContainer");
        const index = container.children.length;

        const div = document.createElement("div");
        div.className = "input-group mb-2";

        const input = document.createElement("input");
        input.type = "text";
        input.name = `productLinks[${index}].url`;
        input.className = "form-control editField";

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "btn btn-danger editField";
        removeBtn.innerText = "-";
        removeBtn.onclick = function () {
            assetDetailsService.removeProductLink(removeBtn);
        };

        div.appendChild(input);
        div.appendChild(removeBtn);
        container.appendChild(div);
    }

    this.removeProductLink = function(btn) {
        const div = btn.parentNode;
        div.remove();
        assetDetailsService.reindexProductLinks();
    }

    this.reindexProductLinks = function() {
        const container = document.getElementById("productLinksEditContainer");
        const children = container.children;

        for (let i = 0; i < children.length; i++) {
            const input = children[i].querySelector('input[name^="productLinks"]');
            if (input) {
                input.name = `productLinks[${i}].url`;
            }
        }
    }

}

function AssetDpiaService() {
    this.dpiaViewElem = null;
    this.dpiaOptOutViewElem = null;

    this.init = function() {
        this.dpiaViewElem = document.getElementById('dpiaView');
        this.dpiaOptOutViewElem = document.getElementById('dpiaOptOutTextView');
        this.setDpiaVisibility(!asset.dpiaOptOut);
    }

    this.setDpiaVisibility = function (visible) {
        this.dpiaViewElem.style.display = visible ? 'block' : 'none';
        this.dpiaOptOutViewElem.style.display = visible ? 'none' : 'block';
    }

    this.setDpiOptOut = function (checkboxElem) {
        let optOut = checkboxElem.checked;
        asset.dpiaOptOut = optOut;
        let optOutBoolean = optOut ? 'true' : 'false';
        assetDetailsService.setField('dpiaOptOut', optOutBoolean);
        // this.updateDpiaBadges();
        this.setDpiaVisibility(!optOut);
    }

    this.updateDpiaOptOutReason = function(elem) {
        assetDetailsService.setField('dpiaOptOutReason', elem.value);
    }

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

}