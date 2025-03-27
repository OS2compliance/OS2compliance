function AssetDpiaService() {
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
        this.initTabRestore()
        this.screeningBadgeElem = document.getElementById("dpiaBadge2");
        this.recommendationElem = document.getElementById("recommendation");
        this.recommendationCardElem = document.getElementById("recommendationCard");
        this.handleAnswerChange();
        this.initQualityAssuranceCheckboxes()

    }

    this.initTabRestore = async ()=> {
        const namespace = 'dpia_details_active_tab'

        //restore saved tab, if any
        const savedID = sessionStorage.getItem(namespace)
        if (savedID) {
            const tabContainer = document.querySelector(".tab-base")
            const tabs = tabContainer.querySelectorAll('.nav-link')
            const tabContents = tabContainer.querySelectorAll('.tab-pane')
            let contentId;
            for (const tab of tabs) {
                if (tab.id === savedID) {
                    tab.classList.add('active')
                    contentId = tab.getAttribute('data-bs-target').substring(1)
                } else {
                    tab.classList.remove('active')
                }
            }
            for (const content of tabContents) {
                if (content.id === contentId) {
                    content.classList.add('active')
                } else {
                    content.classList.remove('active')
                }
            }
        }

        //add event listeners to navtabs
        const tabContainer = document.getElementById("tabs")
        const navLinks = tabContainer.querySelectorAll('.nav-link')
        for (const link of navLinks) {
            link.addEventListener('click', ()=> {
                sessionStorage.setItem(namespace, link.id)
            })
        }
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


    this.initDpia = ()=> {
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
            choiceService.initUserSelect('sendReportTo');
        }
    }

    this.initQualityAssuranceCheckboxes = ()=> {
        const container = document.getElementById('qualityCheckboxesContainer')
        container.addEventListener('change', async (event)=>{
            const target = event.target
            if (target.type==="checkbox" && target.classList.contains('qualityCheck')) {
                const checkedBoxValues = [...container.querySelectorAll('.qualityCheck')]
                    .filter( (checkboxInput)=> checkboxInput.checked)
                    .map( (checkboxInput) => checkboxInput.value)
                const data = {
                    "assetId": assetId,
                    "dpiaQualityCheckValues": checkedBoxValues
                }

                const url = `${restUrl}/qualityassurance/update`
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
        })
    }
}
