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

}
