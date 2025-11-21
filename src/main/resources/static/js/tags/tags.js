import {initColorPickerListener} from "./tag-color-picker.js";

const tags = new TagService()
let token = document.getElementsByName("_csrf")[0].getAttribute("content");


document.addEventListener("DOMContentLoaded", function (event) {

    initColorPickerListener('createTagColorPicker')

    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    const grid = new gridjs.Grid({
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
                id: "title",
                name: "Tag"
            },
            {
                id: "color",
                name: "Farve",
                formatter: (cell, row) => {
                    const span = document.createElement("span");
                    span.className = 'tag-badge'
                    span.textContent = cell.label;
                    span.style.backgroundColor = cell.colorCode
                    span.style.color = cell.contrastCode
                    return gridjs.html(span.outerHTML)
                }
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const tag = row.cells[1]['data'];

                    // Create container
                    const container = document.createElement('div');

                    // Create Edit button
                    const editBtn = document.createElement('button');
                    editBtn.type = 'button';
                    editBtn.className = 'btn btn-icon btn-outline-light btn-xs me-1';
                    editBtn.dataset.action = 'edit';
                    editBtn.dataset.id = id;
                    editBtn.dataset.tag = tag;

                    const editIcon = document.createElement('i');
                    editIcon.className = 'pli-pencil fs-5';
                    editBtn.appendChild(editIcon);

                    // Create Delete button
                    const deleteBtn = document.createElement('button');
                    deleteBtn.type = 'button';
                    deleteBtn.className = 'btn btn-icon btn-outline-light btn-xs me-1';
                    deleteBtn.dataset.action = 'delete';
                    deleteBtn.dataset.id = id;
                    deleteBtn.dataset.tag = tag;

                    const deleteIcon = document.createElement('i');
                    deleteIcon.className = 'pli-trash fs-5';
                    deleteBtn.appendChild(deleteIcon);

                    // Append buttons to container
                    container.appendChild(editBtn);
                    container.appendChild(deleteBtn);

                    return gridjs.html(container.outerHTML);
                }
            }
        ],
        data: data,
        language: {
            'noRecordsFound': "Ingen data fundet",
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
    })

    const datatableId = "tagsDatatable"
    const datatableContainerElement = document.getElementById(datatableId)
    grid.render(datatableContainerElement);

    datatableContainerElement.addEventListener('click', (e) => {
        const button = e.target.closest('button[data-action]');
        if (!button) {
            return;
        }

        const action = button.dataset.action;
        const id = button.dataset.id;
        const tag = button.dataset.tag;

        if (action === 'edit') {
            tags.editTag(id, tag);
        } else if (action === 'delete') {
            tags.deleteTag(id, tag);
        }
    });

    initSaveAsExcelButtonWithDefaultGrid('tagsDatatable', 'Tags')
});


function TagService() {
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
                    {method: "DELETE", headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.editTag = async (id, value) => {

        const container = document.getElementById("editTagModalContainer");

        const networkService = new NetworkService();
        await networkService.GetFragment(`/admin/tags/${id}`, container)

        document.getElementById('editIdentifier').value = id;
        document.getElementById('redigerNavn').value = value;

        initColorPickerListener('editTagColorPicker')

        let editDialog = new bootstrap.Modal(document.getElementById('editTagModal'));
        editDialog.show();
    }
}