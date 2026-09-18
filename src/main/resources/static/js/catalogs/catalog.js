import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";

window.catalog = new CatalogService();

let token = document.getElementsByName("_csrf")[0].getAttribute("content");
let editDialog;
let copyDialog;

const columnProperties = [
    'identifier',
    'name',
    'threatCount',
    'hidden',
    'inUse'
];

document.addEventListener("DOMContentLoaded", function (event) {
    initGrid();
});

function initGrid() {
    fetch(formUrl)
        .then(response => response.text()
            .then(data => document.getElementById('createCatalogDialog').innerHTML = data))
        .catch(error => toastService.error(error));

    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    const gridConfig = {
        className: defaultClassName,
        sort: {
            enabled: true,
            multiColumn: false
        },
        columns: [
            {
                name: "identifier",
                hidden: true
            },
            {
                name: "Katalog",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const identifier = row.cells[columnProperties.indexOf('identifier')]['data'];
                    const url = viewUrl + identifier;
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                name: "Trusler",
                searchable: {
                    sortKey: 'threatCount'
                }
            },
            {
                name: "Synlighed",
                width: '160px',
                searchable: {
                    searchKey: 'hidden',
                    fieldId: 'catalogHiddenSearchSelector'
                },
                formatter: (cell, row) => {
                    const isHidden = cell === true || cell === 'true';
                    return isHidden ? "Skjult" : "Synlig";
                }
            },
            {
                name: "inUse",
                hidden: true
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const identifier = row.cells[columnProperties.indexOf('identifier')]['data'];
                    const catalogName = row.cells[columnProperties.indexOf('name')]['data'].replaceAll("'", "\\'");
                    const inUse = row.cells[columnProperties.indexOf('inUse')]['data'] === true || row.cells[columnProperties.indexOf('inUse')]['data'] === 'true';
                    const deleteStyle = inUse ? 'visibility: hidden' : '';
                    const editButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="catalog.editCatalog('${identifier}')"><i class="pli-pencil fs-5"></i></button>`;
                    const deleteButton = `<button style="${deleteStyle}" type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="catalog.deleteCatalog('${identifier}', '${catalogName}')"><i class="pli-trash fs-5"></i></button>`;
                    const copyButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="catalog.copyCatalog('${identifier}')"><i class="pli-data-copy fs-5"></i></button>`;
                    return gridjs.html(editButton + copyButton + deleteButton);
                }
            }
        ],
        server: {
            url: gridCatalogsUrl,
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
            'noRecordsFound': "Ingen data fundet",
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

    const grid = new gridjs.Grid(gridConfig).render(document.getElementById("catalogsDatatable"));

    // Initialized search, pagination and so forth. Mutates specific parts of table config
    const customGridFunctions = new CustomGridFunctions(grid, gridCatalogsUrl, 'catalogsDatatable', {
        sortDirection: 'ASC',
        sortColumn: 'name',
    });

    initSaveAsExcelButton(customGridFunctions, 'threatCatalog', 'catalogs', 'Trusselskataloger');
}

function CatalogService() {

    this.deleteCatalog = function (identifier, name) {
        Swal.fire({
            text: `Er du sikker på du vil slette trusselskataloget '${name}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`${restUrl}/${identifier}`,
                    {method: "DELETE", headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.editCatalog = function (identifier) {
        fetch(`${edit}?id=${identifier}`)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('editCatalogDialog');
                    dialog.innerHTML = data;
                    editDialog = new bootstrap.Modal(document.getElementById('editCatalogDialog'));
                    editDialog.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.copyCatalog = function (identifier) {
        fetch(`${copyFormUrl}?id=${identifier}`)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('copyCatalogDialog');
                    dialog.innerHTML = data;
                    copyDialog = new bootstrap.Modal(document.getElementById('copyCatalogDialog'));
                    copyDialog.show();
                }))
            .catch(error => toastService.error(error));
    }

}
