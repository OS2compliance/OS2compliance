import OnUnSubmittedService from "../on-unsubmitted-changes-service.js";

let managersChoicesEditSelect = null;
let suppliersChoicesEditSelect = null;
let responsibleChoicesEditSelect = null;
let operationResponsibleChoicesEditSelect = null;
let departmentChoices = null;

let onUnSubmittedService = new OnUnSubmittedService();

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", function (event) {
    initDatePickers();
    suppliersChoicesEditSelect = initSuppliersSelect();
    suppliersChoicesEditSelect.disable();

    managersChoicesEditSelect = choiceService.initUserSelect("managers", false);
    managersChoicesEditSelect.disable();
    responsibleChoicesEditSelect = choiceService.initUserSelect("responsibleUsers", false);
    responsibleChoicesEditSelect.disable();
    operationResponsibleChoicesEditSelect = choiceService.initUserSelect("operationResponsible", false);
    operationResponsibleChoicesEditSelect.disable();
    departmentChoices = choiceService.initOUSelect('departmentSelect', false);
    departmentChoices.disable();
    editMode(false);
    initDpia();

    addRelationFormLoaded();
    initTabs();
    rememberSelectedTab();

    if (threatExists) {
        profilePageLoaded();
    }

    const aiStatusSelect = document.getElementById('aiStatus');
    const riskFactorContainer = document.getElementById('riskFactorContainer');

    function toggleRiskFactorContainer() {
        if (aiStatusSelect.value === 'YES') {
            riskFactorContainer.style.display = '';
        } else {
            riskFactorContainer.style.display = 'none';
        }
    }
    toggleRiskFactorContainer();
    aiStatusSelect.addEventListener('change', toggleRiskFactorContainer);

    initEditButtons();
});

function initEditButtons() {
    const editButton = document.getElementById('editAssetBtn');
    const cancelButton = document.getElementById('cancelBtn');
    const editForm = document.getElementById('editForm');

    editButton?.addEventListener('click', () => {
        const responsibleFieldsCanBeChanged = editButton.dataset.responsibleChangeable;
        editMode(true, responsibleFieldsCanBeChanged);
        onUnSubmittedService.setChangesMade();
    });
    cancelButton?.addEventListener('click', () => {
        editMode(false, false);
        onUnSubmittedService.reset();
    });
    editForm?.addEventListener('submit', () => {
        onUnSubmittedService.reset();
    });
}

function initTabs() {
    const myTabs = document.querySelectorAll("ul.nav-tabs > li > button");
    const panes = document.querySelectorAll(".tab-pane");
    const tabAction = Object.keys(myTabs).map((tab) => {
        myTabs[tab].addEventListener("click", (e) => {

            makeInactive(myTabs);
            activateTab(e);
            makeInactive(panes);
            activateTabContent(e);
            //save selected tab
            window.localStorage.setItem('activeTab', '#' + myTabs[tab].getAttribute('data-bs-target').substr(1));
            e.preventDefault();
        });
    });

}

function initAssetRelationSelect() {
    const relationsSelect = document.getElementById('AssetRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    // choiceService.updateRelationsAssetsOnly(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsAssetsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
relationsSelect.addEventListener("change",
    function(event) {
        choiceService.updateRelationsAssetsOnly(relationsChoice, "");
    },
    false,
    );
}

function addRelationFormLoaded() {
    initAssetRelationSelect();
    initDocumentRelationSelectPrivate();
    initTaskRelationSelectPrivate();
    initRegisterRelationSelectPrivate();
    initPrecautionRelationSelectPrivate();
    initIncidentRelationSelectPrivate();
}

function initRegisterRelationSelectPrivate() {
    const relationsSelect = document.getElementById('RegisterRelationModalrelationsSelect')
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsRegistersOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsRegistersOnly(relationsChoice, "");
        },
        false,
    );
}

 function initPrecautionRelationSelectPrivate() {
    const relationsSelect = document.getElementById('PrecautionRelationModalrelationsSelect')
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsPrecautionsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsPrecautionsOnly(relationsChoice, "");
        },
        false,
    );
}

function initDocumentRelationSelectPrivate() {
    const relationsSelect = document.getElementById('DocumentRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsDocumentsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsDocumentsOnly(relationsChoice, "");
        },
        false,
    );
}

function initTaskRelationSelectPrivate() {
    const relationsSelect = document.getElementById('TaskRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsTasksOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
    function(event) {
        choiceService.updateRelationsTasksOnly(relationsChoice, "");
    },
    false,
    );
}

function initIncidentRelationSelectPrivate() {
    const relationsSelect = document.getElementById('IncidentRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search", (event) => {
            choiceService.updateRelationsIncidentsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change", (event) => {
            choiceService.updateRelationsIncidentsOnly(relationsChoice, "");
        },
        false,
    );
}

function rememberSelectedTab() {
    // on load of the page: switch to the currently selected tab
    //var hash = window.location.hash;
    var hash = window.localStorage.getItem('activeTab');

    if (hash != null && hash !== "") {
        //Inactivate all tabs
        const myTabs = document.querySelectorAll("ul.nav-tabs > li > button");
        makeInactive(myTabs);

        //Hide all contentTabs
        const panes = document.querySelectorAll(".tab-pane");
        makeInactive(panes);

        const activeTab = document.querySelector('button[data-bs-target="' + hash + '"]');
        activeTab?.classList.add("active");

        const activePane = document.querySelector(hash);
        activePane?.classList.add("active");
        activePane?.classList.add("show");
    }

}

function initDatePickers() {
    if (!isKitos) {
        const contractDatePicker = MCDatepicker.create({
            el: '#contractDate',
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller"
        });
        document.querySelector( "#contractDateBtn" ).addEventListener( "click", () => {
            contractDatePicker.open();
        });

        const contractTerminationPicker = MCDatepicker.create({
            el: '#contractTermination',
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller"
        });
        document.querySelector( "#contractTerminationBtn" ).addEventListener( "click", () => {
            contractTerminationPicker.open();
        });
    }
}

function removeChoiceByUserUuid(arr, uuid) {
    const objWithIdIndex = arr.findIndex((obj) => obj.uuid === uuid);

    if (objWithIdIndex > -1) {
        arr.splice(objWithIdIndex, 1);
    }

    return arr;
}

function removeChoiceByRegisterId(arr, id) {
    const objWithIdIndex = arr.findIndex((obj) => obj.id === id);
    if (objWithIdIndex > -1) {
        arr.splice(objWithIdIndex, 1);
    }

    return arr;
}

function updateSuppliers(choices, search) {
    fetch( `/rest/suppliers/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                alreadySelected = []
                var sel = choices.passedElement.element;
                for (var i=0, n=sel.options.length;i<n;i++) {
                    if (sel.options[i].value) alreadySelected.push(sel.options[i].value);
                }
                alreadySelected.forEach((id) => removeChoiceByRegisterId(data.content, id));

                choices.setChoices(data.content.map(e => {
                    return {
                        value: e.id,
                        label: `${e.name}`}
                }), 'value', 'label', true); //false to not override already selected choices
            }))
        .catch(error => toastService.error(error));
}

function initSuppliersSelect() {
    let supplierSelect = document.getElementById('supplier');
    const supplierChoices = initSelect(supplierSelect);
    supplierSelect.addEventListener("search",
        function(event) {
            updateSuppliers(supplierChoices, event.detail.value);
        },
        false,
    );
    supplierChoices.setChoiceByValue(supplierId)
    return supplierChoices;
}

function editMode(enabled, responsibleFieldChangeable) {
    const rootElement = document.getElementById('editForm');
    const aiStatus = document.getElementById('aiStatus');
    const active = document.getElementById('activeAsset');
    const aiRiskFactor = document.getElementById('riskFactor');
    if (enabled) {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = false;
            if (elem.tagName === "A") {
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

        document.getElementById('saveEditAssetBtn').hidden = false;
        document.getElementById('cancelBtn').hidden = false;
        document.getElementById('editAssetBtn').hidden = true;

        if (!isKitos) {
            if (responsibleFieldChangeable=== 'true') {
                managersChoicesEditSelect.enable();
                responsibleChoicesEditSelect.enable();
                operationResponsibleChoicesEditSelect.enable();
            }
            active.disabled = false;
            document.getElementById("productLinksViewContainer").hidden = true;
            document.getElementById("productLinksEditContainer").hidden = false;
            document.getElementById("addProductLinkBtn").hidden = false;
        }

        suppliersChoicesEditSelect.enable();
        if (responsibleFieldChangeable === 'true') {
            responsibleChoicesEditSelect.enable();
        }

        departmentChoices.enable();
    } else {
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = true;
            if (elem.tagName === "A") {
                elem.hidden = false;
                elem.nextElementSibling.hidden = true;
            }

            if (elem.classList.contains("datepicker")) {
                if (elem.value == null || elem.value === "") {
                    elem.parentElement.hidden = true; // hide the datepicker
                    elem.parentElement.nextElementSibling.hidden = false; // show the "ingen" textfield
                }
            }
        });

        const saveEditAssetBtn = document.getElementById('saveEditAssetBtn')
        const cancelBtn =  document.getElementById('cancelBtn')
        const editAssetBtn = document.getElementById('editAssetBtn')
        if (saveEditAssetBtn) {
            saveEditAssetBtn.hidden = true;
        }
        if (cancelBtn) {
            cancelBtn.hidden = true;
        }
        if (editAssetBtn) {
            editAssetBtn.hidden = false;
        }

        suppliersChoicesEditSelect.disable();
        managersChoicesEditSelect.disable();
        responsibleChoicesEditSelect.disable();
        operationResponsibleChoicesEditSelect.disable();

        if (!isKitos) {
            active.disabled = true;
            document.getElementById("productLinksViewContainer").hidden = false;
            document.getElementById("productLinksEditContainer").hidden = true;
            document.getElementById("addProductLinkBtn").hidden = true;
        }

        departmentChoices.disable();
    }
    if (isKitos) {
        aiStatus.disabled = true;
        aiRiskFactor.disabled = true;
    }
}