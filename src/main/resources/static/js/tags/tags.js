const tags = new TagService()
let token = document.getElementsByName("_csrf")[0].getAttribute("content");


document.addEventListener("DOMContentLoaded", function(event) {
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
                name: "Id",
                hidden: true
            },
            {
                id: "value",
                name: "Tag"
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const tag = row.cells[1]['data'];
                    const deleteButton = `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="tags.deleteTag('${id}', '${tag}')"><i class="pli-trash fs-5"></i></button>`;
                    return gridjs.html(deleteButton);
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
                'results': 'Tags',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    }).render(document.getElementById("tagsDatatable"));

    initSaveAsExcelButton()
});


function TagService () {
    this.deleteTag = (id, name) => {
        Swal.fire({
            text: `Er du sikker på du vil slette dette tag: '${name}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`${restUrl}/${id}`,
                    {method: "DELETE", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }
}

function initSaveAsExcelButton() {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click",  () => exportHtmlTableToExcel('tagsDatatable', 'Tags'))
}