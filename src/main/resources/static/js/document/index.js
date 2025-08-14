
const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

function createDocumentFormLoaded() {
    initDatepicker("#nextRevisionBtn", "#nextRevision");
    userChoicesEditSelect = choiceService.initUserSelect('userSelect');
    choiceService.initDocumentRelationSelect();
    choiceService.initTagSelect('createDocumentTagsSelect');

    userChoicesEditSelect.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoicesEditSelect);
    });
    initFormValidationForForm("createDocumentModal", () => validateChoices(userChoicesEditSelect));
}

document.addEventListener("DOMContentLoaded", function(event) {
    createDocumentFormLoaded();

    initGrid()

    initGridActions()

});

function deleteClicked(documentId, name) {
    Swal.fire({
        text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra DOCUMENT slettes også.`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#03a9f4',
        cancelButtonColor: '#df5645',
        confirmButtonText: 'Ja',
        cancelButtonText: 'Nej'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`${deleteUrl}${documentId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
        }
    })
}

function initGridActions() {
    delegateListItemActions(
        'documentsDatatable',
        (id)=>{}, // No editing.
        (id, name) => deleteClicked(id, name)
        )
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
                    searchKey: 'name',
                },
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                name: "Dokumentype",
                searchable: {
                    searchKey: 'documentType',
                    fieldId: "documentTypeStatusSearchSelector"
                },
                width: '150px'
            },
            {
                name: "Ansvarlig",
                searchable: {
                    searchKey: 'responsibleUser.name',
                },
            },
            {
                name: "Næste revidering",
                searchable: {
                    searchKey: 'nextRevision',
                },
                width: '120px',
            },
            {
                name: "Status",
                searchable: {
                    searchKey: 'status',
                    fieldId: "statusSearchSelector"
                },
                width: '150px',
                formatter: (cell, row) => {
                    var status = cell;
                    if (cell === "Ikke startet") {
                        status = '<div class="d-block badge bg-warning">' + cell + '</div>';
                    } else if (cell === "I gang") {
                        status = '<div class="d-block badge bg-info">' + cell + '</div>';
                    } else if (cell === "Klar") {
                        status = '<div class="d-block badge bg-success">' + cell + '</div>';
                    }

                    return gridjs.html(status, 'div')
                },
            },
            {
                id: 'allowedActions',
                name: 'Handlinger',
                sort: 0,
                width: '120px',
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
        server:{
            url: gridDocumentsUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(document =>
                [ document.id, document.name, document.documentType, document.responsibleUser, document.nextRevision, document.status, document.allowedActions ]
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
    const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "documentsDatatable" ));

    //Enables custom column search, serverside sorting and pagination
    const customGridFunctions = new CustomGridFunctions(grid, gridDocumentsUrl, 'documentsDatatable');

    initSaveAsExcelButton(customGridFunctions)

    gridOptions.init(grid, document.getElementById("gridOptions"));
}

function initSaveAsExcelButton(customGridFunctions) {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click",  () => exportGridServerSide(customGridFunctions, 'Dokumenter'))
}