const catalog = new CatalogService();

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
                    {method: "DELETE", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                    .then(response => location.reload())
                    .catch(error => toastService.error(error));
            }
        });
    }

    this.editCatalog = function(identifier) {
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

    this.copyCatalog = function(identifier) {
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
