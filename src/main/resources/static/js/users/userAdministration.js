const users = new UserService()
let token = document.getElementsByName("_csrf")[0].getAttribute("content");

function UserService () {
    let selectedUserUuid


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
        fetchUserDetailPartial(this.getSelectedUserUuid())
    }

    this.onEditAssetRole = async (event, assetId, userUuid)=> {
        const addAssetUrl = `${viewUrl}/${userUuid}/role/${assetId}`;
        const modalId = 'addAssetRoleModal'
        await fetchHtml(addAssetUrl, modalId)


        const modalcontainer = document.getElementById(modalId)
        const addAssetRoleForm = modalcontainer.querySelector('#editRoleForm')

        addAssetRoleForm.addEventListener('submit', (event)=>this.submitAssetRole(event, addAssetRoleForm, userUuid, assetId))

        const modal = new bootstrap.Modal(modalcontainer);
        modal.show();
    }

    this.onAddAssetRole = async (event, assetId) => {
        const targetId = 'editRoleModalContainer'
        const url = '/asset/roles/create/' + assetId

        //Fetch fragment from server
        fetchHtml(url, targetId)
            .then( ()=> {
                    //show modal
                    const modalContent = document.getElementById('editRoleModal')
                    const form = modalContent.querySelector('#editRoleModalForm')
                    form.addEventListener('submit', (event)=> this.onAddAssetRoleFormSubmit(event, form, null))

                    choiceService.initUserSelect('roleUserSelect')

                    const modal = new bootstrap.Modal(modalContent);
                    modal.show();
            })
    }

    this.onAddAssetRoleFormSubmit = async (event, form, roleId)=> {
        event.preventDefault();
        const assetId = form.getAttribute('data-assetId')
        const formData = new FormData(form)

        const data = {
             roleDTO : {
                id : roleId,
                name : formData.get('name'),
                assetId : assetId,
            },
            userIds : formData.getAll('roleUserSelect')
         }

        await postData('/rest/asset/roles/edit', data)

        location.reload()

    }

    this.saveSelectedUserUuid = (uuid) => {
        sessionStorage.setItem('admin_users_selectedUser', uuid)
    }

    this.getSelectedUserUuid = ()=> {
        return sessionStorage.getItem('admin_users_selectedUser')
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
//        sort: {
//            enabled: true,
//            multiColumn: false
//        },
        columns: [
            {
                id: "uuid",
                name: "Id",
                hidden: true
            },
            {
                id: "userId",
                name: "Bruger",
                searchable : {
                    searchKey: 'userId'
                },
                formatter: (cell, row) => {
                    return gridjs.html( `<span data-user-uuid="${row.cells[0].data}">${cell}</span>` )
                }
            },
            {
                id: "name",
                name: "Navn",
                searchable : {
                    searchKey: 'name'
                },
            },
            {
                id: "email",
                name: "Email",
                 searchable : {
                     searchKey: 'email'
                 },
            },
             {
                id: "accessRole",
                name: "Rettigheder",
//                searchable : {
//                    searchKey: 'accessRole',
//                    fieldId : 'userRoleSearchSelector'
//                },
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
        server: {
            url: restUrl + "/all",
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(user =>
                [user.uuid, user.userId, user.name, user.email, user.accessRole, user.active]
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
                'results': 'Brugere',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    })

    grid.render(document.getElementById("userDatatable"));

    //Enables custom column search, serverside sorting and pagination
    new CustomGridFunctions(grid, restUrl + "/all", 'userDatatable')

    //On row element click listener
    grid.on('rowClick', (...args) => onGridRowClick(...args));

    //load last selected user, if any
    const selectedUuid = users.getSelectedUserUuid()
    if (selectedUuid) {
        fetchUserDetailPartial(selectedUuid)
    }
});


function onGridRowClick(args) {
    const rowElement = args.target.closest('.gridjs-tr')
    const uuid = rowElement.querySelector('[data-user-uuid]').getAttribute('data-user-uuid')

    users.saveSelectedUserUuid(uuid)
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

async function onAddAsset() {
    const targetId = 'editRoleModalContainer'
    const url = '/assets/form'

    const formUrl = '/assets/form';
    fetch(formUrl)
    .then(response => response.text()
        .then(data => {
            const formDialog = document.getElementById('formDialog')
            formDialog.innerHTML = data;
            formLoaded();

            const modal = new bootstrap.Modal(formDialog);
            modal.show();
        }))
    .catch(error => toastService.error(error))
}

function formLoaded() {
    initUserChoices();
    initSuppliersChoices();
    initFormValidationForFormChoicesOnly("createForm", () => validateChoices(userChoices));
}

function initUserChoices() {
    const userSelect = document.getElementById('userSelect');
    userChoices = initSelect(userSelect);
    choiceService.updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            choiceService.updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    userChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(userChoices);
    });
}

function initSuppliersChoices() {
    const supplierSelect = document.getElementById('supplierSelect');
    supplierChoices = initSelect(supplierSelect);
    choiceService.updateSuppliers(supplierChoices, "");
    supplierSelect.addEventListener("search",
        function(event) {
            choiceService.updateSuppliers(supplierChoices, event.detail.value);
        },
        false,
    );
    supplierChoices.passedElement.element.addEventListener('change', function() {
        checkInputField(supplierChoices);
    });
}
