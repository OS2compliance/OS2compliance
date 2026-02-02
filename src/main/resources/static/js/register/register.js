import ColumnOptions from "../grid-js-extension/column-options.js";
import formatTags from "../tags/tag-grid-formatter.js";
import { formatThreatTypes, formatThreatCatalogs, formatRiskAssessment } from "../risk-assessment-formatter.js";
import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";

let grid = null;

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

document.addEventListener("DOMContentLoaded", function (event) {
    initGrid()

    initGridActionButtons()

    initPageTopButtons()
});

function initPageTopButtons() {
    const createRegisterButton = document.getElementById("createRegisterButton");
    createRegisterButton?.addEventListener("click",  () => createRegisterService.show())
}

function initGridActionButtons() {
    delegateListItemActions(
        "registersDatatable",
        (id) => editRegisterService.show(id),
        (id, name)=> deleteClicked(id, name)
    )
}

function deleteClicked(registerId, name) {
    Swal.fire({
        text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra REGISTER slettes også.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`${deleteUrl}${registerId}`, {method: 'DELETE', headers: {'X-CSRF-TOKEN': token}})
                .then(() => {
                    window.location.reload();
                });
        }
    })
}

function initGrid() {
    let gridConfig = {
        className: defaultClassName,
        columns: [
            {
                name: "id",
                hidden: true
            },
            {
                name: "Titel",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                name: "Afdeling",
                searchable: {
                    searchKey: 'responsibleOUNames'
                },
            },
            {
                name: "Forvaltning",
                searchable: {
                    searchKey: 'departmentNames'
                },
            },
            {
                name: "Kontakt",
                searchable: {
                    searchKey: 'responsibleUserNames'
                },
            },
            {
                name: "Opdateret",
                searchable: {
                    searchKey: 'updatedAt'
                },
                width: "100px"
            },
            {
                name: "Konsekvens vurdering",
                searchable: {
                    searchKey: 'consequence',
                    fieldId: 'registerConsequenceSearchSelector'
                },
                width: "110px",
                formatter: (cell, row) => {
                    let assessment = cell;
                    if (cell === "Grøn") {
                        assessment = '<div class="badge bg-green align-top" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Lysgrøn") {
                        assessment = '<div class="badge bg-green-300 align-top" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Gul") {
                        assessment = '<div class="badge bg-yellow-500 align-top" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Orange") {
                        assessment = '<div class="badge bg-orange align-top" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Rød") {
                        assessment = '<div class="badge bg-red align-top" style="width: 60px">' + cell + '</div>';
                    }
                    return gridjs.html(assessment, 'div');
                }
            },
            {
                name: "Risiko vurdering",
                searchable: {
                    searchKey: 'risk',
                    fieldId: 'registerRiskSearchSelector'
                },
                width: "150px",
                formatter: (cell, row) => {
                    let assessment = '';
                    if (cell === "Grøn") {
                        assessment = '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Lysgrøn") {
                        assessment = '<div class="d-block badge bg-green-300" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Gul") {
                        assessment = '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Orange") {
                        assessment = '<div class="d-block badge bg-orange" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Rød") {
                        assessment = '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>';
                    }
                    return gridjs.html(assessment, 'div');
                },
            },
            {
                name: "Risiko aktiver",
                searchable: {
                    searchKey: 'assetAssessment',
                    fieldId: 'registerAssetSearchSelector'
                },
                width: "150px",
                formatter: (cell, row) => {
                    let assessment = '';
                    if (cell === "Grøn") {
                        assessment = '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Lysgrøn") {
                        assessment = '<div class="d-block badge bg-green-300" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Gul") {
                        assessment = '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Orange") {
                        assessment = '<div class="d-block badge bg-orange" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Rød") {
                        assessment = '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>';
                    }
                    return gridjs.html(assessment, 'div');
                },
            },
            {
                name: "Status",
                searchable: {
                    searchKey: 'status',
                    fieldId: 'registerStatusSearchSelector'
                },
                width: '150px',
                formatter: (cell, row) => {
                    let status = cell;
                    if (cell === "Klar") {
                        status = '<div class="d-block badge bg-success" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "I gang") {
                        status = '<div class="d-block badge bg-info" style="width: 60px">' + cell + '</div>';
                    } else if (cell === "Ikke startet") {
                        status = '<div class="d-block badge bg-danger" style="width: 60px">' + cell + '</div>';
                    }
                    else {
                        status = '<div class="d-block badge bg-gray" style="width: 60px">' + cell + '</div>';
                    }
                    return gridjs.html(status, 'div');
                },
            },
            {
                name: "Aktiver",
                width: "100px"
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
                width: '200px',
                formatter: (cell, row) => formatThreatTypes(cell)
            },
            {
                name: "Risikokataloger",
                hidden: true,
                searchable: {
                    searchKey: 'catalogList'
                },
                width: '200px',
                formatter: (cell, row) => formatThreatCatalogs(cell)
            },
            {
                name: "riskScore",
                hidden: true
            },
            {
                name: "Gennemsnitlig risiko",
                hidden: true,
                width: '150px',
                searchable: {
                    sortKey: 'riskScore'
                },
                formatter: (cell, row) => {
                    const riskData = row.cells[16]['data'];
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
                width: '90px',
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    const name = row.cells[1]['data'].replaceAll("'", "\\'");
                    const attributeMap = new Map();
                    attributeMap.set('identifier', identifier);
                    attributeMap.set('name', name);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                }
            }
        ],
        server: {
            url: gridRegistersUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(register => {
                const riskData = {
                    avgProbability: register.avgProbability,
                    avgConsequenceOverall: register.avgConsequenceOverall,
                    avgConsequenceConfidentialityRegistered: register.avgConsequenceConfidentialityRegistered,
                    avgConsequenceConfidentialityOrganisation: register.avgConsequenceConfidentialityOrganisation,
                    avgConsequenceConfidentialitySociety: register.avgConsequenceConfidentialitySociety,
                    avgConsequenceIntegrityRegistered: register.avgConsequenceIntegrityRegistered,
                    avgConsequenceIntegrityOrganisation: register.avgConsequenceIntegrityOrganisation,
                    avgConsequenceIntegritySociety: register.avgConsequenceIntegritySociety,
                    avgConsequenceAvailabilityRegistered: register.avgConsequenceAvailabilityRegistered,
                    avgConsequenceAvailabilityOrganisation: register.avgConsequenceAvailabilityOrganisation,
                    avgConsequenceAvailabilitySociety: register.avgConsequenceAvailabilitySociety,
                    avgConsequenceAuthenticitySociety: register.avgConsequenceAuthenticitySociety
                };

                const riskScore = (register.avgProbability && register.avgConsequenceOverall)
                    ? (register.avgProbability * register.avgConsequenceOverall).toFixed(2)
                    : null;

                return [
                    register.id,
                    register.name,
                    register.responsibleOUs,
                    register.departments,
                    register.responsibleUsers,
                    register.updatedAt,
                    register.consequence,
                    register.risk,
                    register.assetAssessment,
                    register.status,
                    register.assetCount,
                    register.tags,
                    register.threatTypeList,
                    register.catalogList,
                    riskScore,
                    null, // placeholder for risk assessment formatter
                    riskData, // hidden column with all risk data
                    register.allowedActions
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
                'results': 'Fortegnelser',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const registerDatatableId = 'registersDatatable';
    grid = new gridjs.Grid(gridConfig).render(document.getElementById(registerDatatableId));

    const customGridFunctions = new CustomGridFunctions(grid, gridRegistersUrl, exportRegistersUrl, registerDatatableId);

    new ColumnOptions(
        registerDatatableId,
        grid,
        ['titel', 'allowedActions'],
        ['titel','risikoVurdering','status', 'aktiver'],
        ['id', 'riskScore', 'riskData'])

    initSaveAsExcelButton(customGridFunctions, 'register', 'registers', 'Fortegnelse')

}