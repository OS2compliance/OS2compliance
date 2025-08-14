let editDialog;

document.addEventListener("DOMContentLoaded", async function (event) {
    const form = document.getElementById('formDialog')
    if (form) {
        await fetch(formUrl).then(response => response.text()
            .then(data => {
                form.innerHTML = data
            }))
            .catch(error => toastService.error(error));
    }

    initGrid()

    initAllowedActions()

    initPageTopButtons()
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
                [supplier.id, supplier.name, supplier.solutionCount, supplier.updated, supplier.status, supplier.allowedActions]
            ),
            total: data => data.count
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
    const grid = new gridjs.Grid(gridConfig).render(document.getElementById("suppliersDatatable"));

    const customGridFunctions = new CustomGridFunctions(grid, gridSuppliersUrl, 'suppliersDatatable');

    initSaveAsExcelButton(customGridFunctions)

    gridOptions.init(grid, document.getElementById("gridOptions"));
}

function initPageTopButtons() {
    const createRegisterButton = document.getElementById("createRegisterButton");
    createRegisterButton.addEventListener("click",  () => createRegisterService.show())
}

function initSaveAsExcelButton(customGridFunctions) {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click",  () => exportGridServerSide(customGridFunctions, 'Leverandører'))
}