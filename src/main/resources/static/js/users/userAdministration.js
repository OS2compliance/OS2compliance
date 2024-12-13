const users = new UserService()
let token = document.getElementsByName("_csrf")[0].getAttribute("content");

function UserService () {
    this.deleteUser = (id, name) => {
        Swal.fire({
            text: `Er du sikker på du vil slette brugeren '${name}'?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`${restUrl}/delete/${id}`,
                    {method: "DELETE", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.openEditModal = function(id, action) {
        const url = id ? `${viewUrl}/${action}/${id}` : `${viewUrl}/${action}`
        fetch(url)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('addUserModal');
                    dialog.innerHTML = data;
                    editDialog = new bootstrap.Modal(document.getElementById('addUserModal'));
                    editDialog.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.resetUser = (id) => {
        Swal.fire({
            text: `Er du sikker på du vil resette brugerens password?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`${restUrl}/reset/${id}`,
                    {method: "POST", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => toastService.info("Reset password link sendt til brugeren"))
                    .catch(error => toastService.error(error));
            }
        });
    }


    this.submitAssetRole = async(event, form, uuid, assetId) => {
        event.preventDefault()

        //Submit assets

        const checkBoxes = document.querySelectorAll('.roleInput:checked')
        const data = {
            assetId :assetId,
            selectedRoleIds: [...checkBoxes].map(box => box.id.split('_')[1])
        }

        const url = `${restUrl}/${uuid}/role/add`;
        await postData(url, data)

        //refresh detail view
        fetchUserDetailPartial(uuid)
    }

    this.onAddAssetRole = async (event, assetId, userUuid)=> {
        const addAssetUrl = `${viewUrl}/${userUuid}/role/${assetId}`;
        const modalId = 'addAssetRoleModal'
        await fetchHtml(addAssetUrl, modalId)


        const modalcontainer = document.getElementById(modalId)
        const addAssetRoleForm = modalcontainer.querySelector('#addRoleForm')

        addAssetRoleForm.addEventListener('submit', (event)=>this.submitAssetRole(event, addAssetRoleForm, userUuid, assetId))

        const modal = new bootstrap.Modal(modalcontainer);
        modal.show();
    }


}

document.addEventListener("DOMContentLoaded", function(event) {
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
                id: "uuid",
                name: "Id",
                hidden: true
            },
            {
                id: "userId",
                name: "Bruger",
                formatter: (cell, row) => {
                    return gridjs.html( `<span data-user-uuid="${row.cells[0].data}">${cell}</span>` )
                }
            },
            {
                id: "name",
                name: "Navn"
            },
            {
                id: "email",
                name: "Email"
            },
             {
                id: "accessRole",
                name: "Rettigheder",
                formatter: (cell, row) => {
                    return roleOptions.find(roleOption => roleOption.value === cell).display;
                }
            },
            {
                id: "active",
                name: "Aktiv",
                formatter: (cell, row) => {
                    return gridjs.html(cell === true ? '<div class="d-block badge bg-success">Ja</div>' : '<div class="d-block badge bg-danger">Nej</div>');
                }
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '100px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const userId = row.cells[1]['data'];
                    const resetButton = row.cells[4].data === 'noaccess' ? '' : `<button type="button" title="Reset brugerens password" class="btn btn-icon btn-outline-light btn-xs me-1"  onclick="users.resetUser('${id}')"><i class="pli-security-remove fs-5"></i></button>`;
                    const editButton = `<button type="button" title="Rediger" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="users.openEditModal('${id}', 'edit')"><i class="pli-pencil fs-5"></i></button>`;
                    const deleteButton = `<button type="button" title="Slet" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="users.deleteUser('${id}', '${userId}')"><i class="pli-trash fs-5"></i></button>`;
                    return gridjs.html(`<div class='d-flex gap-1 justify-content-end'>${resetButton}${editButton}${deleteButton}</div>`);
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
                'results': 'Brugere',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    })

    grid.render(document.getElementById("userDatatable"));

    //On row element click listener
    grid.on('rowClick', (...args) => onGridRowClick(...args));
});


function onGridRowClick(args) {
    const rowElement = args.target.closest('.gridjs-tr')
    const uuid = rowElement.querySelector('[data-user-uuid]').getAttribute('data-user-uuid')

    fetchUserDetailPartial(uuid)
}

async function fetchUserDetailPartial (uuid) {
    const userDetailURL = viewUrl + '/' + uuid
    const containerId = 'userDetailContainer'
    await fetchHtml(userDetailURL, containerId)
    initDetailPartial(uuid)
}

function initDetailPartial (uuid) {
    //init the note input
    const noteInput = document.getElementById('userNote')
    new InputTimer(noteInput, 1500, async ()=> {
        const data = {
            note: noteInput.value
        }
        const url = restUrl  + '/' + uuid + '/note'
        putData(url, data)
    })

}

