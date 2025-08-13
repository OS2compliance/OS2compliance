const columnProperties = [
    'id',
    'name',
    'responsibleOUName',
    'responsibleUserName',
    'userUpdatedDate',
    'taskCount',
    'reportApprovalStatus',
    'screeningConclusion',
    'isExternal',
    'allowedActions'
];
let createDPIAService, createExternalDPIAService, editDPIAService;

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

document.addEventListener("DOMContentLoaded", function(event) {
    createDPIAService = new CreateDPIAService()
    createDPIAService.init()
    editDPIAService = new EditDPIAService()

    createExternalDPIAService = new CreateExternalDPIAService()

    initGrid()

});

function deleteClicked(dpiaId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?\nReferencer afhængige af konsekvensvurderingen slettes også.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteUrl}/${dpiaId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
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
                name: "Titel",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const url = baseUrl + "/" + row.cells[0]['data'];
                    const isExternal = row.cells[8]['data']; //last cell in row contains the external boolean
                    if (isExternal) {
                        return gridjs.html(`<a href="${url}" target="_blank">${cell} (Ekstern)</a>`);
                    } else {
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    }
                }
            },
            {
                name: "Fagområde",
                searchable: {
                    searchKey: 'responsibleOuName'
                },
            },
            {
                name: "Risikoejer",
                searchable: {
                    searchKey: 'responsibleUserName'
                },
            },
            {
                name: "Godkendelsesdato",
                searchable: {
                    searchKey: 'userUpdatedDate'
                },
                formatter: (date) => {
                    if (date) {
                        const dateObj = new Date(date);
                        return dateObj.toLocaleString('da-DK', {
                            timeZone: 'Europe/Copenhagen',
                            year: "numeric",
                            month: "2-digit",
                            day: "2-digit",
                        }).replaceAll('.', '-').replace('-', '/');
                    }
                    return '';
                },
                sort: {
                    compare: (a, b) => {
                        return new Date(b) < new Date(a);
                    }
                },
                width: '150px'
            },
            {
                name: "Opgaver",
                searchable: {
                    sortKey: 'taskCount'
                },
                width: '100px'
            },
            {
                name: "Status",
                width: '120px',
                searchable: {
                    searchKey: 'reportApprovalStatus',
                    fieldId: 'dpiaStatusSearchSelector',
                },
            },
            {
                name: "Screening",
                width: '120px',
                searchable: {
                    searchKey: 'screeningConclusion',
                    fieldId: 'screeningConclusionSearchSelector',
                },
                formatter: (cell, row) => {
                    let html = ``;
                    if (cell) {
                        if (cell === 'RED') {
                            html = `<span class="badge bg-red">&nbsp</span>`;
                        }
                        if (cell === 'YELLOW') {
                            html = `<span class="badge bg-yellow">&nbsp</span>`;
                        }
                        if (cell === 'GREEN') {
                            html = `<span class="badge bg-green">&nbsp</span>`;
                        }
                        if (cell === 'GREY') {
                            html = `<span class="badge bg-gray-800">&nbsp</span>`;
                        }
                    }
                    return  gridjs.html(html);
                },
            },
            {
                id: 'isExternal',
                hidden: true,
            },
            {
                id: 'allowedActions',
                name: 'Handlinger',
                sort: 0,
                width: '100px',
                formatter: (cell, row) => {
                    console.log(cell)
                    console.log(row)
                    const identifier = row.cells[0]['data'];
                    const name = row.cells[1]['data'].replaceAll("'", "\\'");
                    const external = row.cells[8]['data']
                    const attributeMap = new Map();
                    attributeMap.set('identifier', identifier);
                    attributeMap.set('name', name);
                    attributeMap.set('external', external);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                }
            }
        ],
        server:{
            url: listDataUrl,
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
                'results': 'Konsekvensanalyser',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "dpiaDatatable" ));
    const customGridFunctions = new CustomGridFunctions(grid, listDataUrl, 'dpiaDatatable');
    window.customGridFunctions = customGridFunctions;
    gridOptions.init(grid, document.getElementById("gridOptions"));

    initGridListItemButtons()
}

function initGridListItemButtons() {
    delegateListItemActions('dpiaDatatable',
        (id, elem) => {
            if (elem.dataset.external) {
                createExternalDPIAService.editExternalClicked(uid)
            } else {
                editDPIAService.openModal(id)
            }
        },
        (id, name, elem) => deleteClicked(id, name)
        )
}
