import ColumnOptions from "../grid-js-extension/column-options.js";
import formatTags from "../tags/tag-grid-formatter.js";
import { initSaveAsExcelButton } from "../excel-export/excel-export-init.js";

let editDialog;

document.addEventListener("DOMContentLoaded", async function (event) {
    const form = document.getElementById('formDialog')
    if (form) {
        await fetch(formUrl).then(response => response.text()
            .then(data => {
                form.innerHTML = data
                initFormValidationForForm('createForm');
            }))
            .catch(error => toastService.error(error));
    }

    initGrid()

    initAllowedActions()
});

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};


function deleteClicked(supplierId, name) {
    Swal.fire({
        text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra leverandøren slettes også.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`${deleteUrl}${supplierId}`, {method: 'DELETE', headers: {'X-CSRF-TOKEN': token}})
                .then(() => {
                    window.location.reload();
                });
        }
    })
}

function editClicked(supplierId) {
    let dialog = document.getElementById('formEditDialog');
    if (dialog) {
        fetch(`${formUrl}?id=${supplierId}`)
            .then(response => response.text()
                .then(data => {
                    dialog.innerHTML = data;
                    editDialog = new bootstrap.Modal(document.getElementById('formEditDialog'));
                    editDialog.show();
                    initFormValidationForForm('editForm');
                }))
            .catch(error => toastService.error(error));
    }
}


function initAllowedActions() {
    delegateListItemActions(
        "suppliersDatatable",
        (id) => editClicked(id),
        (id, name) => deleteClicked(id, name),
    )
}

function initGrid() {
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
                name: "Navn",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    const uuid = row.cells[6]['data'];
                    if (uuid) {
                        return gridjs.html(`<a href="${url}">${cell}</a> <img src="/img/kitos_icon.svg" alt="OS2kitos Logo" width="40">`);
                    }
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                },
                width: '40%'
            },
            {
                name: "Antal løsninger",
                width: '20%',
                searchable: {
                    sortKey: 'solutionCount'
                }
            },
            {
                name: "Opdateret",
                searchable: {
                    searchKey: 'updated'
                },
                width: '100px'
            },
            {
                name: "Sidste tilsyn",
                searchable: {
                    searchKey: 'lastOversightDate'
                },
                width: '90px',
                formatter: (cell, row) => {
                    if (!cell || cell.trim() === '') {
                        return gridjs.html(`<span>-</span>`);
                    }

                    var dateParts = cell.split('-');
                    if (dateParts.length === 3) {
                        var formattedDate = `${dateParts[2]}/${dateParts[1]}-${dateParts[0]}`;
                        return gridjs.html(`<span>${formattedDate}</span>`);
                    }

                    return gridjs.html(`<span>${cell}</span>`);
                }
            },
            {
                name: "Status",
                searchable: {
                    searchKey: 'status',
                    fieldId: 'supplierStatusSearchSelector'
                },
                width: '100px',
                formatter: (cell, row) => {
                    var status = cell;
                    if (cell === "Klar") {
                        status = [
                            '<div class="d-block badge bg-success">' + cell + '</div>'
                        ]
                    } else if (cell === "I gang") {
                        status = [
                            '<div class="d-block badge bg-info">' + cell + '</div>'
                        ]
                    }
                    return gridjs.html(''.concat(...status), 'div')
                },
            },
            {
                name: "kitosUuid",
                hidden: true
            },
            {
                name: "Tags",
                searchable: {
                    searchKey: 'tagNames',
                },
                formatter: (cell, row) => formatTags(cell, row),
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
            url: gridSuppliersUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(supplier =>
                [supplier.id, supplier.name, supplier.solutionCount, supplier.updated, supplier.lastOversightDate, supplier.status, supplier.kitosUuid, supplier.tags, supplier.allowedActions]
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
                'results': 'leverandører',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const datatableId ='suppliersDatatable'
    const grid = new gridjs.Grid(gridConfig).render(document.getElementById(datatableId));

    const customGridFunctions = new CustomGridFunctions(grid, gridSuppliersUrl, datatableId);

    initSaveAsExcelButton(customGridFunctions, 'supplier', 'suppliers', 'Leverandører')

    new ColumnOptions(
        datatableId,
        grid,
        ['navn', 'allowedActions'],
        ['navn', 'allowedActions','antalLøsninger', 'opdateret','status' ],
        ['id', 'kitosUuid'])
}