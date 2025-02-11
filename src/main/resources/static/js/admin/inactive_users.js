let transferToChoice, transferFromChoice, transferFromSelect, transferToSelect, transferResponsibilityModal;

function transferResponsibility() {
    let transferFrom = transferFromSelect.value;
    let transferTo = transferToSelect.value;
    console.log("from " + transferFrom + " to " + transferTo);

    let data = {
                 "transferFrom": transferFrom,
                 "transferTo": transferTo
               };

    postData(`/rest/admin/transferresponsibility`, data).then((response) => {
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }
        toastService.info("Ansvaret er overført");
        document.querySelector('#transferResponsibilityModal .btn-close').click();
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    }).catch(error => {toastService.error(error)});
}

function initModalWithDefaultTransferFrom(elem) {
    let uuid = elem.dataset.uuid;
    let name = elem.dataset.name;
    transferFromChoice.setChoices([{
        label: name,
        value: uuid,
        selected: true
    }]);
    var transferResponsibilityBootstrapModal = new bootstrap.Modal(transferResponsibilityModal);
    transferFromChoice.disable();

    transferResponsibilityBootstrapModal.show();
}

function pageLoaded() {

    transferFromSelect = document.getElementById('transferFrom');
    if(transferFromSelect !== null) {
        transferFromChoice = choiceService.initUserSelect('transferFrom', false);
    }
    transferToSelect = document.getElementById('transferTo');
    if(transferToSelect !== null) {
        transferToChoice = choiceService.initUserSelect('transferTo', false);
    }

    transferResponsibilityModal = document.getElementById('transferResponsibilityModal');
    transferResponsibilityModal.addEventListener('hidden.bs.modal', function () {
        transferFromChoice.removeActiveItems();
        transferToChoice.removeActiveItems();
        transferFromChoice.enable();
    });

    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    new gridjs.Grid({
        className: defaultClassName,
        sort: {
            enabled: true,
            multiColumn: false
        },
        columns: [
            {
                id: "uuid",
                name: "Uuid",
                hidden: true
            },
            {
                id: "name",
                name: "Navn"
            },
            {
                id: "userId",
                name: "Brugernavn"
            },
            {
                id: "responsibleFor",
                name: "Ansvarlig for",
                sort: 0,
                formatter: (cell, row) => {
                    let htmlResponsibleFor = '<ul>';
                    row.cells[3].data.forEach(responsibleFor => {
                        let url = "";
                        switch (responsibleFor.type) {
                            case "SUPPLIER":
                                url = `/suppliers/${responsibleFor.id}`;
                                break;
                            case "CONTACT":
                                url = `/contacts/${responsibleFor.id}`;
                                break;
                            case "TASK":
                                url = `/tasks/${responsibleFor.id}`;
                                break;
                            case "DOCUMENT":
                                url = `/documents/${responsibleFor.id}`;
                                break;
                            case "REGISTER":
                                url = `/registers/${responsibleFor.id}`;
                                break;
                            case "ASSET":
                                url = `/assets/${responsibleFor.id}`;
                                break;
                            case "THREAT_ASSESSMENT":
                                url = `/risks/${responsibleFor.id}`;
                                break;
                            case "STANDARD_SECTION":
                                url = `/standards/supporting/${responsibleFor.id}`;
                                break;
                            default:
                                url = "#"; // Fallback link, hvis typen ikke er genkendt
                                break;
                        }
                        console.log(responsibleFor)
                        htmlResponsibleFor += `<li><a href="${url}">${responsibleFor.typeMessage}: ${responsibleFor.name}</a></li>`;
                    });
                    htmlResponsibleFor += "</ul>";
                    return gridjs.html(htmlResponsibleFor);
                }
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const uuid = row.cells[0]['data'];
                    const name = row.cells[1]['data'];
                    const transferButton = `<button type="button" title="Overfør ansvar" class="btn btn-icon btn-xs me-1" data-name="${name}" data-uuid="${uuid}" onclick="initModalWithDefaultTransferFrom(this)"><i class="ti-angle-double-right fs-5"></i></button>`;
                    return gridjs.html(transferButton);
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
                'results': 'Opgaver',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    }).render(document.getElementById("inactiveUsersDatatable"));
}
