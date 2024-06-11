const precaution = new PrecautionService();

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
                    {method: "DELETE", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.editPrecaution = function(identifier) {
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
