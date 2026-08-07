import ColumnOptions from "../grid-js-extension/column-options.js";
import formatTags from "../tags/tag-grid-formatter.js";
import { formatThreatTypes, formatThreatCatalogs, formatRiskAssessment } from "../risk-assessment-formatter.js";
import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";
import { formatColorStatus } from "./asset-color-status-formatter.js";

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

const badgeClassByAssessment = {
    'Grøn': 'bg-green',
    'Lysgrøn': 'bg-green-300',
    'Gul': 'bg-yellow-500',
    'Orange': 'bg-orange',
    'Rød': 'bg-red'
};

const badgeClassByStatus = {
    'Ikke startet': 'bg-warning',
    'I gang': 'bg-info',
    'Klar': 'bg-success'
};

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

function formatShortDate(cell) {
    const span = document.createElement('span');

    if (!cell || cell.trim() === '') {
        span.textContent = '-';
        return gridjs.html(span.outerHTML);
    }

    const dateParts = cell.split('-');
    if (dateParts.length === 3) {
        span.textContent = `${dateParts[2]}/${dateParts[1]}-${dateParts[0]}`;
        return gridjs.html(span.outerHTML);
    }

    span.textContent = cell;
    return gridjs.html(span.outerHTML);
}


document.addEventListener("DOMContentLoaded", function(event) {
    initFormDialog();
    initGrid();
    initGridActionButtons();
});

function initFormDialog() {
    const dialog = document.getElementById('formDialog')
    if (dialog) {
        fetch(formUrl)
            .then(response => {
                if (response.ok) {
                    response.text()
                        .then(data => {
                            dialog.innerHTML = data;
                            formLoaded();
                            //initFormValidationForForm('formDialog');
                        })
                }
            })
            .catch(error => {
                toastService.error(error)
            })
    }
}

function deleteClicked(assetId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra aktivet slettes også.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteUrl}${assetId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
}

async function onEditClicked(assetId) {
    const response = await fetch(`${formUrl}?id=${assetId}`, {
        headers: {
            'X-CSRF-TOKEN': token
        }
    })

    if (!response.ok) {
        toastService.error(response.error)
        console.error("Could not load edit fragment for asset")
    }

    const responseText = await response.text()

    let dialog = document.getElementById('formDialog');
    dialog.innerHTML = responseText;
    const editDialog = new bootstrap.Modal(document.getElementById('formDialog'));
    editDialog.show();

}

function initGrid() {
    let assetGridConfig = {
        className: defaultClassName,
        columns: [
            {
                name: "id",
                hidden: true
            },
            {
                name:"kitos",
                hidden: true
            },
            {
                name: "Navn",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];

                    const container = document.createElement('span');
                    const link = document.createElement('a');
                    link.href = url;
                    link.textContent = cell;
                    container.appendChild(link);

                    if (row.cells[1]['data'] == 'true') {
                        container.appendChild(document.createTextNode(' '));
                        const img = document.createElement('img');
                        img.src = '/img/kitos_icon.svg';
                        img.alt = 'OS2kitos Logo';
                        img.width = 40;
                        if (row.cells[12]['data'] == true) {
                            img.classList.add('grayscale');
                        }
                        container.appendChild(img);
                    }

                    return gridjs.html(container.innerHTML);
                }
            },
            {
                name: "Leverandør",
                searchable: {
                    searchKey: 'supplier'
                }
            },
            {
                id: 'asset',
                name: "Aktiv/Inaktiv",
                searchable: {
                    searchKey: 'active',
                    fieldId : 'activeAssetSelector'
                },
                formatter: (cell, row) => {
                    if (cell) {
                        return 'Ja';
                    } else {
                        return 'Nej';
                    }
                }
            },
            {
                name: "Tredjelandsoverførsel",
                searchable: {
                    searchKey: 'hasThirdCountryTransfer',
                    fieldId : 'assetThirdCountrySelector'
                },
                formatter: (cell, row) => {
                    if (cell) {
                        return 'Ja';
                    } else {
                        return 'Nej';
                    }
                }
            },
            {
                name: "Type",
                searchable: {
                    searchKey: 'assetType'
                },
            },
            {
                name: "Systemejer",
                searchable: {
                    searchKey: 'responsibleUserNames'
                },
            },
            {
                name: "Systemansvarlig",
                searchable: {
                    searchKey: 'managerUserNames'
                }
            },
            {
                name: "Opdateret",
                searchable: {
                    searchKey: 'updatedAt'
                },
            },
            {
                name: "Sidste tilsyn",
                searchable: {
                    searchKey: 'lastOversightDate'
                },
                formatter: (cell, row) => formatShortDate(cell)
            },
            {
                name: "Antal beh.",
                searchable: {
                    sortKey: 'registers'
                },
            },
            {
                id: 'threatAssessment',
                name: "Risikovurdering",
                searchable: {
                    searchKey: 'assessment',
                    fieldId:'assetRiskSearchSelector'
                },
                formatter: (cell, row) => {
                    if (!badgeClassByAssessment.hasOwnProperty(cell)) {
                        return gridjs.html('', 'div');
                    }

                    const badge = document.createElement('div');
                    badge.className = `d-block badge badge-style-sm ${badgeClassByAssessment[cell]}`;
                    badge.textContent = cell;

                    return gridjs.html(badge.outerHTML, 'div')
                },
            },
            {
                name: "Status",
                searchable: {
                    searchKey: 'assetStatus',
                    fieldId : 'assetStatusSearchSelector'
                },
                formatter: (cell, row) => {
                    if (!badgeClassByStatus.hasOwnProperty(cell)) {
                        return gridjs.html(cell, 'div');
                    }

                    const badge = document.createElement('div');
                    badge.className = `d-block badge ${badgeClassByStatus[cell]}`;
                    badge.textContent = cell;

                    return gridjs.html(badge.outerHTML, 'div');
                },
            },
            {
                name: "Tags",
                searchable: {
                    searchKey: 'tagNames',
                },
                formatter: (cell, row) => formatTags(cell, row),
            },
            {
                name: "Trusselstyper",
                hidden: true,
                searchable: {
                    searchKey: 'threatTypeList'
                },
                formatter: (cell, row) => formatThreatTypes(cell)
            },
            {
                name: "Risikokataloger",
                hidden: true,
                searchable: {
                    searchKey: 'catalogList'
                },
                formatter: (cell, row) => formatThreatCatalogs(cell)
            },
            {
                name: "riskScore",
                hidden: true
            },
            {
                name: "Gennemsnitlig risiko",
                hidden: true,
                searchable: {
                    sortKey: 'riskScore'
                },
                formatter: (cell, row) => {
                    const riskData = row.cells[19]['data'];
                    return formatRiskAssessment(cell, row, riskData);
                }
            },
            {
                name: "riskData",
                hidden: true
            },
            {
                id: 'allowedActions',
                name: 'Handlinger',
                sort: 0,
                formatter: (cell, row) => {
                    const attributeMap = new Map();
                    const identifier = row.cells[0]['data'];
                    attributeMap.set('identifier', identifier);
                    const name = row.cells[2]['data'];
                    attributeMap.set('name', name);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                }
            },
            {
                id: 'departments',
                hidden: true,
                name: "Ansvarlige forvaltninger",
                searchable: {
                    searchKey: 'departments'
                }
            },
            {
                id: 'assetCategory',
                hidden: true,
                name: "Kategori",
                searchable: {
                    searchKey: 'assetCategory'
                },
                formatter: (cell, row) => formatColorStatus(cell)
            },
            {
                id: 'description',
                hidden: true,
                name: "Beskrivelse",
                searchable: {
                    searchKey: 'description'
                },
                formatter: (cell, row) => {
                    if (!cell) {
                        return '';
                    }

                    const span = document.createElement('span');
                    span.className = 'd-inline-block text-truncate';
                    span.style.maxWidth = '250px';
                    span.title = cell;
                    span.textContent = cell;

                    return gridjs.html(span.outerHTML);
                }
            },
            {
                id: 'operationResponsible',
                hidden: true,
                name: "Driftsansvarlig",
                searchable: {
                    searchKey: 'operationResponsibleUsers'
                }
            },
            {
                id: 'criticality',
                hidden: true,
                name: "Kritikalitet",
                searchable: {
                    searchKey: 'criticality',
                    sortKey: 'criticalityOrder'
                }
            },
            {
                id: 'sociallyCritical',
                hidden: true,
                name: "Samfundskritisk",
                searchable: {
                    searchKey: 'sociallyCritical'
                },
                formatter: (cell, row) => cell ? 'Ja' : 'Nej'
            },
            {
                id: 'aiStatus',
                hidden: true,
                name: "Anvender løsningen AI",
                searchable: {
                    searchKey: 'aiStatus'
                }
            },
            {
                id: 'contractDate',
                hidden: true,
                name: "Kontraktdato",
                searchable: {
                    searchKey: 'contractDate'
                },
                formatter: (cell, row) => formatShortDate(cell)
            },
            {
                id: 'contractTermination',
                hidden: true,
                name: "Kontraktophør",
                searchable: {
                    searchKey: 'contractTermination'
                },
                formatter: (cell, row) => formatShortDate(cell)
            },
            {
                id: 'terminationNotice',
                hidden: true,
                name: "Opsigelsesvarsel",
                searchable: {
                    searchKey: 'terminationNotice'
                }
            },
            {
                id: 'dataProcessingAgreementStatus',
                hidden: true,
                name: "Er der indgået databehandleraftale",
                searchable: {
                    searchKey: 'dataProcessingAgreementStatus'
                }
            },
            {
                id: 'dataProcessingAgreementDate',
                hidden: true,
                name: "Databehandleraftale dato",
                searchable: {
                    searchKey: 'dataProcessingAgreementDate'
                },
                formatter: (cell, row) => formatShortDate(cell)
            },
            {
                id: 'securityMeasuresStatus',
                hidden: true,
                name: "Vurdering af foranstaltninger",
                searchable: {
                    sortKey: 'assetMeasureStatusOrder'
                },
                formatter: (cell, row) => formatColorStatus(cell)
            },
            {
                id: 'riskAssessmentOptOutStatus',
                hidden: true,
                name: "Risikovurdering fravalgt",
                searchable: {
                    sortKey: 'riskAssessmentOptOutStatusOrder'
                },
                formatter: (cell, row) => formatColorStatus(cell)
            },
            {
                id: 'dpiaStatus',
                hidden: true,
                name: "DPIA",
                searchable: {
                    sortKey: 'dpiaStatusOrder'
                },
                formatter: (cell, row) => formatColorStatus(cell)
            },
            {
                id: 'tiaStatus',
                hidden: true,
                name: "TIA vurdering",
                searchable: {
                    sortKey: 'tiaStatusOrder'
                },
                formatter: (cell, row) => formatColorStatus(cell)
            },
            {
                id: 'archive',
                hidden: true,
                name: "Systemet skal arkiveres",
                searchable: {
                    searchKey: 'archive'
                }
            }
        ],
        server:{
            url: gridAssetsUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(asset => {
                const riskData = {
                    avgProbability: asset.avgProbability,
                    avgConsequenceOverall: asset.avgConsequenceOverall,
                    avgConsequenceConfidentialityRegistered: asset.avgConsequenceConfidentialityRegistered,
                    avgConsequenceConfidentialityOrganisation: asset.avgConsequenceConfidentialityOrganisation,
                    avgConsequenceConfidentialitySociety: asset.avgConsequenceConfidentialitySociety,
                    avgConsequenceIntegrityRegistered: asset.avgConsequenceIntegrityRegistered,
                    avgConsequenceIntegrityOrganisation: asset.avgConsequenceIntegrityOrganisation,
                    avgConsequenceIntegritySociety: asset.avgConsequenceIntegritySociety,
                    avgConsequenceAvailabilityRegistered: asset.avgConsequenceAvailabilityRegistered,
                    avgConsequenceAvailabilityOrganisation: asset.avgConsequenceAvailabilityOrganisation,
                    avgConsequenceAvailabilitySociety: asset.avgConsequenceAvailabilitySociety,
                    avgConsequenceAuthenticitySociety: asset.avgConsequenceAuthenticitySociety
                };

                // Calculate risk score (probability × consequence) for sorting/searching
                const riskScore = (asset.avgProbability && asset.avgConsequenceOverall)
                    ? (asset.avgProbability * asset.avgConsequenceOverall).toFixed(2)
                    : null;

                return [
                    asset.id,
                    asset.kitos,
                    asset.name,
                    asset.supplier,
                    asset.active,
                    asset.hasThirdCountryTransfer,
                    asset.assetType,
                    asset.ownedByUsers,
                    asset.responsibleUsers,
                    asset.updatedAt,
                    asset.lastOversightDate,
                    asset.registers,
                    asset.assessment,
                    asset.assetStatus,
                    asset.tags,
                    asset.threatTypeList,
                    asset.catalogList,
                    riskScore, // risk score for sorting/searching
                    null, // placeholder for risk assessment formatter
                    riskData, // hidden column with all risk data
                    asset.allowedActions,
                    asset.departments,
                    asset.assetCategory,
                    asset.description,
                    asset.operationResponsibleUsers,
                    asset.criticality,
                    asset.sociallyCritical,
                    asset.aiStatus,
                    asset.contractDate,
                    asset.contractTermination,
                    asset.terminationNotice,
                    asset.dataProcessingAgreementStatus,
                    asset.dataProcessingAgreementDate,
                    asset.securityMeasuresStatus,
                    asset.riskAssessmentOptOutStatus,
                    asset.dpiaStatus,
                    asset.tiaStatus,
                    asset.archive,
                    asset.oldKitos
                ];
            }),
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
                'results': 'aktiver',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const datatableId ="assetsDatatable"
    const grid = new gridjs.Grid(assetGridConfig).render( document.getElementById( datatableId ));

    const customGridFunctions = new CustomGridFunctions(grid, gridAssetsUrl, datatableId);

    new ColumnOptions(datatableId, grid, ['navn', 'allowedActions'], ['navn', 'allowedActions','type','status' ], ['id', 'kitos', 'riskScore', 'riskData'], '.tableOptionsContainer', 8, true)

    initSaveAsExcelButton(customGridFunctions, 'asset', 'assets', 'Aktiver')
}

function initGridActionButtons() {
    delegateListItemActions(
        "assetsDatatable",
        (id) => onEditClicked(id),
        (id, name)=> deleteClicked(id, name)
    )
}