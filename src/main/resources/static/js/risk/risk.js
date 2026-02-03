import {initStatisticView} from "../statistic/statisticView.js";
import ColumnOptions from "../grid-js-extension/column-options.js";
import formatTags from "../tags/tag-grid-formatter.js";
import {CreateThreatAssessmentService, initRegisterSelect, initAssetSelectRisk, userChanged} from "./createThreatAssessmentService.js";
import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";

const columnProperties = [
    'id',
    'name',
    'type',
    'responsibleOU',
    'responsibleUser',
    'relatedAssetsAndRegisters',
    'tasks',
    'completedTasks',
    'date',
    'threatAssessmentReportApprovalStatus',
    'assessment',
    'threatCatalogs',
    'tags',
    'hidden',
    'allowedActions',
    'fromExternalSource',
    'externalLink'
]

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

let createExternalRiskassessmentService;
const createRiskService = new CreateThreatAssessmentService();
const copyRiskService = new CopyRiskService();
const editRiskService = new EditRiskService();
const createTable = new CreateTable();
const preselect = new Preselect();

document.addEventListener("DOMContentLoaded", async function (event) {
    createExternalRiskassessmentService = new CreateExternalRiskassessmentService(initAssetSelectRisk, initRegisterSelect)
    window.createExternalRiskassessmentService = createExternalRiskassessmentService;

    const table = document.getElementById("risksDatatable");
    if (table) {
        createTable.init();
        initTableActions()
    } else {
        preselect.init();
    }
    createRiskService.init();

    initPageTopButtons()

    await initStatisticView('ThreatAssessment')
});

function Preselect() {
    this.init = function () {
        // Preselect the type
        let type = document.getElementById("threatAssessmentType");
        type.value = "REGISTER";
        type.dispatchEvent(new Event("change"));
    }
}

function initTableActions() {
    delegateListItemActions('risksDatatable',
        (id, elem) => {
            if (elem.dataset.external === 'true') {
                createExternalRiskassessmentService.editExternalClicked(id)
            } else {
                editRiskService.showEditDialog(id)
            }
        },
        (id, name, elem) => deleteClicked(id, name),
        (id, elem) => copyRiskService.showCopyDialog(id),
        (id, elem) => toggleHiddenClicked(id)
    )
}

function toggleHiddenClicked(riskId) {
    fetch(`${restUrl}/${riskId}/toggle-hidden`, {
        method: 'POST',
        headers: {'X-CSRF-TOKEN': token}
    })
    .then(response => {
        if (response.ok) {
            window.location.reload();
        } else {
            toastService.error('Kunne ikke ændre skjult status');
        }
    })
    .catch(error => toastService.error(error));
}

function CreateTable() {
    this.init = function () {
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };

        let gridConfig = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Risikovurdering",
                    searchable: {
                        searchKey: 'name'
                    },
                    formatter: (cell, row) => {
                        const external = row.cells[columnProperties.indexOf('fromExternalSource')]['data']
                        const externalLink = row.cells[columnProperties.indexOf('externalLink')]['data']
                        const url = viewUrl + row.cells[columnProperties.indexOf('id')]['data'];
                        if (external) {
                            return gridjs.html(`<a href="${externalLink}" target="_blank">${cell} (Ekstern)</a>`);
                        } else {
                            return gridjs.html(`<a href="${url}">${cell}</a>`);
                        }
                    }
                },
                {
                    name: "Type",
                    searchable: {
                        searchKey: 'type',
                        fieldId: 'riskThreatAssessmentSearchSelector'
                    },
                },
                {
                    name: "Fagområde",
                    searchable: {
                        searchKey: 'responsibleOU.name'
                    },
                },
                {
                    name: "Risikoejer",
                    searchable: {
                        searchKey: 'responsibleUser.name'
                    },
                },
                {
                    name: "Entitet",
                    searchable: {
                        searchKey: 'relatedAssetsAndRegisters'
                    },
                    formatter: (cell, row) => {
                        const dbsAssetId = row.cells[0]['data'];

                        let items = [];
                        if (typeof cell === "string" && cell.trim() !== "") {
                            items = cell.split("||").map(name => name.trim());
                        } else if (Array.isArray(cell)) {
                            items = cell.map(item => typeof item === "string" ? item.trim() : item.name);
                        }

                        const badges = items.map(option =>
                            `<div class="badge bg-info me-1 mb-1" style="white-space: normal; word-break: break-word; overflow-wrap: break-word; text-align: left">${option}</div>`
                        );

                        return gridjs.html(`<div class="d-flex flex-wrap">${badges.join('')}</div>`);
                    },
                },
                {
                    name: "Opgaver",
                    searchable: {
                        sortKey: 'tasks'
                    },
                },
                {
                    name: "Løste opgaver",
                    searchable: {
                        sortKey: 'completedTasks'
                    },
                },
                {
                    name: "Dato",
                    searchable: {
                        searchKey: 'date'
                    },
                },
                {
                    name: "Status",
                    searchable: {
                        searchKey: 'threatAssessmentReportApprovalStatus',
                        fieldId: 'riskStatusSearchSelector'
                    },
                },
                {
                    name: "Risikovurdering",
                    searchable: {
                        searchKey: 'assessment',
                        fieldId: 'riskAssessmentSearchSelector'
                    },
                    formatter: (cell, row) => {
                        let status = cell;
                        if (cell === "Grøn") {
                            status = [
                                '<div class="d-block badge bg-green">' + cell + '</div>'
                            ]
                        } else if (cell === "Lysgrøn") {
                            status = [
                                '<div class="d-block badge bg-green-300">' + cell + '</div>'
                            ]
                        } else if (cell === "Gul") {
                            status = [
                                '<div class="d-block badge bg-yellow">' + cell + '</div>'
                            ]
                        } else if (cell === "Orange") {
                            status = [
                                '<div class="d-block badge bg-orange">' + cell + '</div>'
                            ]
                        } else if (cell === "Rød") {
                            status = [
                                '<div class="d-block badge bg-danger">' + cell + '</div>'
                            ]
                        } else if (cell === "NONE") {
                            return "";
                        }
                        return gridjs.html(''.concat(...status), 'div')
                    },
                },
                {
                    name: "Trusselskataloger",
                    searchable: {
                        searchKey: 'threatCatalogs',
                    },
                    formatter: (cell, row) => {
                        if (!cell || (cell && cell.trim() === '')) {
                            return gridjs.html('<span class="text-muted">Ingen kataloger</span>');
                        }

                        const catalogs = cell.split(',').map(catalog => catalog.trim()).filter(catalog => catalog !== '');
                        const badges = catalogs.map(catalog => {
                            const truncated = catalog.length > 20 ? catalog.substring(0, 19) + '...' : catalog;
                            return `<span class="badge bg-info me-1 mb-1 small" title="${catalog}">${truncated}</span>`;
                        });
                        return gridjs.html(`<div class="d-flex flex-wrap" style="max-height: 50px; overflow: hidden;">${badges.join('')}</div>`);
                    },
                },
                {
                    id: 'tags',
                    name: "Tags",
                    searchable: {
                        searchKey: 'tagNames',
                    },
                    formatter: (cell, row) => formatTags(cell, row),
                },
                {
                    name: "Skjult",
                    searchable: {
                        searchKey: 'hidden',
                        fieldId: 'riskHiddenSearchSelector'
                    },
                    formatter: (cell, row) => {
                        const isHidden = cell === true || cell === 'true';
                        return gridjs.html(isHidden ? 'Ja' : 'Nej');
                    }
                },
                {
                    id: 'allowedActions',
                    name: 'Handlinger',
                    sort: 0,
                    width: '10%',
                    formatter: (cell, row) => {
                        const identifier = row.cells[columnProperties.indexOf('id')]['data'];
                        const name = row.cells[columnProperties.indexOf('name')]['data'].replaceAll("'", "\\'");
                        const external = row.cells[columnProperties.indexOf('fromExternalSource')]['data'];
                        const hidden = row.cells[columnProperties.indexOf('hidden')]['data'];
                        const attributeMap = new Map();
                        attributeMap.set('identifier', identifier);
                        attributeMap.set('name', name);
                        attributeMap.set('external', external);
                        attributeMap.set('hidden', hidden);
                        return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                    }
                },
                {
                    name: "fromExternalSource",
                    hidden: true
                },
                {
                    name: "externalLink",
                    hidden: true
                },
            ],
            server: {
                url: gridRisksUrl,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(obj => {
                        const result = []
                        for (const property of columnProperties) {
                            result.push(obj[property])
                        }
                        return result;
                    }
                ),
                total: data => data.totalCount
            },
            language: {
                'search': {
                    'placeholder': 'Søg'
                },
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': 'Opgaver',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };
        const grid = new gridjs.Grid(gridConfig).render(document.getElementById("risksDatatable"));

        grid.on('ready', function () {
            // Ensure correct page load behavior
            if (!document.getElementsByClassName("gridjs-currentPage")[0]) {
                document.getElementsByClassName("gridjs-pages")[0].children[1]?.click();
            }

            // Initialize all Entitet Choices.js selects
            Array.from(document.querySelectorAll("[id^='assetsRegistersSelect']"))
                .map(select => select.id)
                .forEach(elementId => {
                    let elementById = document.getElementById(elementId);
                    initSelect(elementById, 'form-control', {readOnly: true});
                });
        });

        const datatableId = 'risksDatatable'
        const customGridFunctions = new CustomGridFunctions(grid, gridRisksUrl, exportRisksUrl, datatableId);

        initSaveAsExcelButton(customGridFunctions,'threatAssessment', 'risks', 'Risikovurderinger');

        new ColumnOptions(
            datatableId,
            grid,
            ['risikovurdering', 'allowedActions'],
            ['risikovurdering', 'allowedActions', 'type', 'status'],
            ['id', 'externalLink', 'fromExternalSource', 'hidden'])
    }
}

function initPageTopButtons() {
    const createButton = document.getElementById("createExternalThreatassessmentButton");
    createButton?.addEventListener("click", () => createExternalRiskassessmentService.createExternalClicked())
}


function deleteClicked(riskId, name) {
    Swal.fire({
        text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra risikovurderingen slettes også.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`${deleteUrl}${riskId}`, {method: 'DELETE', headers: {'X-CSRF-TOKEN': token}})
                .then(() => {
                    window.location.reload();
                });
        }
    })
}

function EditRiskService() {
    this.getScopedElementById = function (id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showEditDialog = function (threatAssessmentId) {
        const container = document.getElementById('editAssessmentContainer');
        fetch(`${baseUrl}${threatAssessmentId}/edit`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function () {
        let self = this;
        this.modalContainer = document.getElementById('editModal');
        this.initTypeSelect();

        const presentSelect = this.getScopedElementById('editPresentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('editPresentAtMeetingSelect');
        }

        this.userChoicesSelect = choiceService.initUserSelect("editUserSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("editOuSelect");

        initFormValidationForForm("editRiskModalForm", () => this.validate());

        this.userChoicesSelect.passedElement.element.addEventListener('change', function () {
            const userUuid = self.userChoicesSelect.passedElement.element.value;
            userChanged(userUuid);
        });

        const copyAssetSelect = this.getScopedElementById('copyAssetSelect');
        if (copyAssetSelect !== null) {
            this.copyAssetChoicesSelect = initAssetSelectRisk(copyAssetSelect);
        }

        const editAssetSelect = this.getScopedElementById('editAssetSelect');
        if (editAssetSelect !== null) {
            this.editAssetChoicesSelect = initAssetSelectRisk(editAssetSelect);
            if (this.editAssetChoicesSelect?.passedElement?.element) {
                this.editAssetChoicesSelect.passedElement.element.addEventListener('change', () => {
                    this.loadAssetSection();
                });
            }
        }

        const editRegisterSelect = this.getScopedElementById('editRegisterSelect');
        if (editRegisterSelect !== null) {
            this.editRegisterChoicesSelect = initRegisterSelect(editRegisterSelect);
        }

        const catalogSelect = this.getScopedElementById('editThreatCatalogSelect');
        initSelectWithConfirmation(catalogSelect);

        this.editAssessmentModal = new bootstrap.Modal(this.modalContainer);
        this.editAssessmentModal.show();
    }

    this.loadAssetSection = function() {
        const selectedAsset = this.getScopedElementById("editAssetSelect").value;

        if (!selectedAsset) {
            return;
        }

        fetch(`/rest/risks/asset?assetIds=${selectedAsset}`)
            .then(response => response.json()
                .then(data => {
                    if (data.elementName) {
                        this.getScopedElementById('editName').value = data.elementName;
                    }

                    let user = data.users?.users[0];
                    if (user) {
                        this.userChoicesSelect.setChoiceByValue(user.uuid);
                    }
                }))
            .catch(error => toastService.error(error));
    }

    this.typeChanged = function(selectedType) {
        const registerRow = document.getElementById("editRegisterSelectRow");
        const assetRow = document.getElementById("editAssetSelectRow");
        const titleRow = document.getElementById("editTitleRow");
        const titleValue = document.getElementById("editName");

        if (selectedType === 'ASSET') {
            if (registerRow) registerRow.style.display = 'none';
            if (titleRow) {
                titleRow.style.display = '';
                if (titleValue) {
                    titleValue.value = '';
                }
            }
            if (assetRow) assetRow.style.display = '';

            if (this.editRegisterChoicesSelect) {
                this.editRegisterChoicesSelect.removeActiveItems();
                this.editRegisterChoicesSelect.passedElement.element.removeAttribute('required');
            }

            if (this.editAssetChoicesSelect) {
                this.editAssetChoicesSelect.passedElement.element.setAttribute('required', 'required');
            }

        } else if (selectedType === 'REGISTER') {
            if (registerRow) registerRow.style.display = '';
            if (assetRow) assetRow.style.display = 'none';
            if (titleRow) {
                titleRow.style.display = '';
                if (titleValue) {
                    titleValue.value = '';
                }
            }

            if (this.editRegisterChoicesSelect) {
                this.editRegisterChoicesSelect.passedElement.element.setAttribute('required', 'required');
            }

            if (this.editAssetChoicesSelect) {
                this.editAssetChoicesSelect.removeActiveItems();
                this.editAssetChoicesSelect.passedElement.element.removeAttribute('required');
            }

        } else {
            if (titleRow) titleRow.style.display = '';
            if (registerRow) registerRow.style.display = 'none';
            if (assetRow) assetRow.style.display = 'none';

            if (this.editAssetChoicesSelect) {
                this.editAssetChoicesSelect.removeActiveItems();
                this.editAssetChoicesSelect.passedElement.element.removeAttribute('required');
            }
            if (this.editRegisterChoicesSelect) {
                this.editRegisterChoicesSelect.removeActiveItems();
                this.editRegisterChoicesSelect.passedElement.element.removeAttribute('required');
            }
        }
    }

    this.initTypeSelect = function() {
        let threatAssessmentTypeElement = this.getScopedElementById("editThreatAssessmentType");
        if (threatAssessmentTypeElement) {
            this.typeChanged(threatAssessmentTypeElement.value);
            threatAssessmentTypeElement.addEventListener('change', () => {
                this.typeChanged(threatAssessmentTypeElement.value);
            });
        }
    }

    this.validate = function () {
        let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        let isAssetTypeSelected = document.getElementById('editThreatAssessmentType').value === 'ASSET'
        if (this.copyAssetChoicesSelect != null && isAssetTypeSelected) {
            result &&= checkInputField(this.copyAssetChoicesSelect, true);
        }
        return result && validateInputFieldLength("editName", 255);
    }
}

function CopyRiskService() {
    this.getScopedElementById = function (id) {
        return this.modalContainer.querySelector(`#${id}`);
    }

    this.showCopyDialog = function (threatAssessmentId) {
        const container = document.getElementById('copyAssessmentContainer');
        fetch(`${baseUrl}${threatAssessmentId}/copy`)
            .then(response => response.text()
                .then(data => {
                    container.innerHTML = data;
                    this.onShown();
                })
            )
            .catch(error => toastService.error(error));
    }

    this.onShown = function () {
        let self = this;
        this.modalContainer = document.getElementById('copyModal');
        const registerSelect = this.getScopedElementById('copyRegisterSelect');
        if (registerSelect !== null) {
            this.registerChoicesSelect = initRegisterSelect(registerSelect);
        }
        const assetSelect = this.getScopedElementById('copyAssetSelect');
        if (assetSelect !== null) {
            this.assetChoicesSelect = initAssetSelectRisk(assetSelect);
        }
        const presentSelect = this.getScopedElementById('copyPresentAtMeetingSelect');
        if (presentSelect !== null) {
            this.presentSelect = choiceService.initUserSelect('copyPresentAtMeetingSelect');
        }

        this.userChoicesSelect = choiceService.initUserSelect("copyUserSelect");
        this.ouChoicesSelect = choiceService.initOUSelect("copyOuSelect");
        initFormValidationForForm("copyRiskModalForm",
            () => this.validate());


        this.userChoicesSelect.passedElement.element.addEventListener('change', function () {
            const userUuid = self.userChoicesSelect.passedElement.element.value;
            userChanged(userUuid);
        });

        this.copyAssessmentModal = new bootstrap.Modal(this.modalContainer);
        this.copyAssessmentModal.show();
    }

    this.validate = function () {
        let result = validateChoices(this.userChoicesSelect, this.ouChoicesSelect);
        if (this.assetChoicesSelect != null) {
            result &= checkInputField(this.assetChoicesSelect, true);
        } else if (this.registerChoicesSelect != null) {
            result &= validateChoices(this.registerChoicesSelect);
        }
        return result;
    }
}


