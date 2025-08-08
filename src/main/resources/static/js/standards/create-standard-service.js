const createStandardService = new CreateStandardService();

function CreateStandardService() {
    token = document.getElementsByName("_csrf")[0].getAttribute("content");
    this.standardModalDialog = null;

    this.openStandardModal = function(id, edit=false) {
        let url = "/standards/form"

        if (edit) {
            url += "/" + id;
        }
        fetch(url)
            .then(response => response.text()
                .then(data => {
                    this.standardModalDialog = document.getElementById('standardFormDialog');
                    this.standardModalDialog.innerHTML = data;
                    const createTaskModal = new bootstrap.Modal(this.standardModalDialog);
                    createTaskModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openDeleteSwal = function (id) {
        if (id) {
            Swal.fire({
                text: "Er du sikker på du vil slette denne standard?",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#03a9f4',
                cancelButtonColor: '#df5645',
                confirmButtonText: 'Ja',
                cancelButtonText: 'Nej'
            }).then((result) => {
                if (result.isConfirmed) {
                    fetch("/rest/standards/delete/" + id, { method: 'POST', headers: { 'X-CSRF-TOKEN': token} })
                        .then(() => {
                            window.location.reload();
                        });
                }
            })
        }
    }
}
