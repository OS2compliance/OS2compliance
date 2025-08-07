const createSectionService = new CreateSectionService();

document.addEventListener("shown.bs.modal", function(event) {
    const form = document.getElementById("headerForm");
    if (form) {
        form.addEventListener("submit", function(event) {
            event.preventDefault();

            const sectionValue = form.elements['section'].value;
            // Validate section format, as we do not allow the sections to be versioned too deep
            const parts = sectionValue.split('.');
            if (parts.length > 3 || (parts.length === 1 && !/^\d+$/.test(sectionValue))) {
                return;
            }
            form.submit();
        });
    }
});

function CreateSectionService() {
    this.sectionModalDialog = null;
    this.headerModalDialog = null;

    this.openRequirementModal = function(element) {
        const id = element.dataset.id;
        fetch(`/standards/section/form/` + id)
            .then(response => response.text()
                .then(data => {
                    this.sectionModalDialog = document.getElementById('sectionFormDialog');
                    this.sectionModalDialog.innerHTML = data;
                    const createSectionModal = new bootstrap.Modal(this.sectionModalDialog);
                    createSectionModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openHeaderModal = function(element, isEdit=false) {
        const id = element.dataset.id;
        let url = "/standards/section/header/form/" + id;
        if (isEdit) {
            const headerId = element.dataset.header;
            url = "/standards/section/header/form/" + id + "/" + headerId;
        }
        fetch(url)
            .then(response => response.text()
                .then(data => {
                    this.headerModalDialog = document.getElementById('headerFormDialog');
                    this.headerModalDialog.innerHTML = data;
                    const headerModal = new bootstrap.Modal(this.headerModalDialog);
                    headerModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.openDeleteSwal = function (element, isheader=true) {
        const id = element.dataset.id;
        const path = isheader === true ? "header/" : "section/";
        const deleteText = isheader === true ? "Er du sikker på du vil slette denne gruppe? Alle underliggende krav vil også slettes" : "Er du sikker på du vil slette dette krav?";
        Swal.fire({
            text: deleteText,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch("/rest/standards/" + path + "delete/" + id, { method: 'POST', headers: { 'X-CSRF-TOKEN': token} })
                    .then(() => {
                        window.location.reload();
                    });
            }
        })
    }

}
