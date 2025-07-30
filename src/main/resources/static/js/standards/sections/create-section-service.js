const createSectionService = new CreateSectionService();

document.addEventListener("shown.bs.modal", function(event) {
    createSectionService.toggleTextAreas();
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

    this.openHeaderModal = function(element) {
        const id = element.dataset.id;

        fetch(`/standards/section/header/form/` + id)
            .then(response => response.text()
                .then(data => {
                    this.headerModalDialog = document.getElementById('headerFormDialog');
                    this.headerModalDialog.innerHTML = data;
                    const headerModal = new bootstrap.Modal(this.headerModalDialog);
                    headerModal.show();
                }))
            .catch(error => toastService.error(error));
    }

    this.toggleNSIS = function () {
        const checkbox = document.getElementById('toggleNSIS');
        if (checkbox.checked) {
            // Toggle the two text fields
            document.getElementById('nsisSmart').style.display = '';
            document.getElementById('nsisPractice').style.display = '';
            document.getElementById('description').style.display = 'none';
        }
        else {
            // Toggle the single text field
            document.getElementById('nsisSmart').style.display = 'none';
            document.getElementById('nsisPractice').style.display = 'none';
            document.getElementById('description').style.display = '';
        }
    }

    this.toggleTextAreas = function () {
        let editors = document.querySelectorAll(`.description`);
        for (let i= 0; i< editors.length; ++i) {
            const elem = editors[i];
            window.CreateCkEditor(elem, editor => {});
        }
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
