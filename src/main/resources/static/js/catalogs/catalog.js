const catalog = new CatalogService();

let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", function (event) {
    initGrid()
    initSaveAsExcelButton()
});

function initSaveAsExcelButton(customGridFunctions) {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click", () => exportHtmlTableToExcel('catalogsDatatable', 'Trusselskataloger'))
}

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

    new gridjs.Grid({
        className: defaultClassName,
        sort: {
            enabled: true,
            multiColumn: false
        },
        columns: [
            {
                id: "identifier",
                name: "identifier",
                hidden: true
            },
            {
                id: "name",
                name: "Katalog",
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                id: "threats",
                name: "Trusler",
                formatter: (cell, row) => {
                    return cell.length;
                }
            },
            {
                id: "hidden",
                name: "Synlighed",
                width: '90px',
                formatter: (cell, row) => {
                    return cell ? "Skjult" : "Synlig";
                }
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    const catalog = row.cells[1]['data'];
                    const inUse = inUseMap[identifier];
                    const deleteStyle = inUse ? 'visibility: hidden' : '';
                    const editButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="catalog.editCatalog('${identifier}')"><i class="pli-pencil fs-5"></i></button>`;
                    const deleteButton = `<button style="${deleteStyle}" type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="catalog.deleteCatalog('${identifier}', '${catalog}')"><i class="pli-trash fs-5"></i></button>`;
                    const copyButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="catalog.copyCatalog('${identifier}')"><i class="pli-data-copy fs-5"></i></button>`;
                    return gridjs.html(editButton + copyButton + deleteButton);
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
    }).render(document.getElementById("catalogsDatatable"));
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
