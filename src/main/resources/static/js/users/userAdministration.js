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
}
