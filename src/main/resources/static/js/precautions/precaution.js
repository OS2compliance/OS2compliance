const precaution = new PrecautionService();

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", function (event) {
    initGrid()
    initSaveAsExcelButton()

});

function initGrid() {
    fetch(formUrl)
        .then(response => response.text()
            .then(data => document.getElementById('createPrecautionDialog').innerHTML = data))
        .catch(error => toastService.error(error));

    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    new gridjs.Grid({
        className: defaultClassName,
        sort: {
            enabled: true,
            multiColumn: false
        },
        columns: [
            {
                id: "id",
                name: "ID",
                hidden: true
            },
            {
                id: "name",
                name: "Foranstaltning"
            },
            {
                id: "description",
                name: "Beskrivelse"
            },
            {
                id: "assets",
                name: "Aktiver",
                sort: 0,
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const relatedAssets = assetRelations.filter((relation) => relation.precautionId == id);
                    var html = '<ul>'
                    for (const relation of relatedAssets) {
                        html += '<li><a href="/assets/' + relation.assetId + '">' + relation.assetName + '</a></li>'
                    }
                    html += '</ul>'
                    return gridjs.html(html);
                }
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const precaution = row.cells[1]['data'];
                    const editButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="precaution.editPrecaution('${id}')"><i class="pli-pencil fs-5"></i></button>`;
                    const deleteButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="precaution.deletePrecaution('${id}', '${precaution}')"><i class="pli-trash fs-5"></i></button>`;
                    return gridjs.html(editButton + deleteButton);
                }
            }
        ],
        data: data,
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
    }).render(document.getElementById("precautionsDatatable"));
}

function initSaveAsExcelButton() {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click",  () => exportHtmlTableToExcel('precautionsDatatable', 'Foranstaltninger'))
}

function PrecautionService() {

    this.deletePrecaution = function (identifier, name) {
        Swal.fire({
            text: `Er du sikker på du vil slette foranstaltningen '${name}'?`,
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

    this.editPrecaution = function (identifier) {
        fetch(`${formUrl}?id=${identifier}`)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('editPrecautionDialog');
                    dialog.innerHTML = data;
                    editDialog = new bootstrap.Modal(document.getElementById('editPrecautionDialog'));
                    editDialog.show();
                }))
            .catch(error => toastService.error(error));
    }

}
