import { initSaveAsExcelButtonClientside } from "/js/excel-export/excel-export-init.js";

let transferToChoice, transferFromChoice, transferFromSelect, transferToSelect, transferResponsibilityModal;

const token = document.getElementsByName("_csrf")[0].getAttribute("content");

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

document.addEventListener("DOMContentLoaded", function(event) {
    pageLoaded();
    buttonHandler();
});

function transferResponsibility() {
    let transferFrom = transferFromSelect.value;
    let transferTo = transferToSelect.value;

    let relatableIds = Array.from(document.querySelectorAll('#entityTable .entity-checkbox:checked'))
        .flatMap(checkbox => checkbox.dataset.ids.split(',').map(Number));

    let data = {
        "transferFrom": transferFrom,
        "transferTo": transferTo,
        "relatableIds": relatableIds
    };

    postData(`/rest/admin/transfer/responsibilities`, data).then((response) => {
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }
        toastService.info("Ansvaret er overført");
        document.querySelector('#transferResponsibilityModal .btn-close').click();
        setTimeout(() => {
            location.reload();
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

    updateEntityListForTransferFrom(uuid);

    transferResponsibilityBootstrapModal.show();
}

// Fetches everything the chosen transferFrom user is actually responsible for and rebuilds the checkbox list
function updateEntityListForTransferFrom(uuid) {
    if (!uuid) {
        buildEntityList([]);
        return;
    }

    fetch(`/rest/admin/responsibilities/${uuid}`)
        .then(response => response.json())
        .then(items => buildEntityList(items))
        .catch(error => toastService.error(error));
}

// Renders one row per responsibility type (e.g. "Aktiv"), with a single checkbox per row
// that selects/deselects every underlying responsibility of that type for transfer.
function buildEntityList(items) {
    const container = document.getElementById('entityTable');
    container.innerHTML = '';

    if (!items.length) {
        container.innerHTML = '<p class="text-muted mb-0">Brugeren er ikke ansvarlig for noget</p>';
        updateSelectedEntityCount();
        return;
    }

    const groups = new Map();
    items.forEach(item => {
        if (!groups.has(item.type)) {
            groups.set(item.type, []);
        }
        groups.get(item.type).push(item.id);
    });

    const rows = Array.from(groups.entries()).map(([type, ids]) => ({
        entityName: items.find(item => item.type === type).typeMessage,
        ids: ids.join(',')
    }));

    const grid = new gridjs.Grid({
        className: defaultClassName,
        columns: [
            {
                id: "entityName",
                name: "Ansvarsområde"
            },
            {
                id: "ids",
                hidden: true
            },
            {
                id: "actions",
                name: "Handling",
                width: '90px',
                formatter: (cell, row) => gridjs.html(`
                    <input class="form-check-input entity-checkbox" type="checkbox" data-ids="${row.cells[1].data}" checked>
                `)
            }
        ],
        data: rows,
        language: {
            'noRecordsFound': "Ingen data fundet"
        }
    }).render(container);

    grid.on('ready', () => updateSelectedEntityCount());
}

function setAllEntitiesChecked(checked) {
    document.querySelectorAll('#entityTable .entity-checkbox').forEach(checkbox => checkbox.checked = checked);
    updateSelectedEntityCount();
}

function updateSelectedEntityCount() {
    const total = document.querySelectorAll('#entityTable .entity-checkbox').length;
    const checked = document.querySelectorAll('#entityTable .entity-checkbox:checked').length;

    const countLabel = document.getElementById('selectedEntityCount');
    if (countLabel) {
        countLabel.textContent = `${checked} af ${total} valgt`;
    }

    const transferButtonCount = document.getElementById('transferButtonCount');
    if (transferButtonCount) {
        transferButtonCount.textContent = checked;
    }

    const toggleAllButton = document.getElementById('toggleAllEntities');
    if (toggleAllButton) {
        toggleAllButton.textContent = checked === total && total > 0 ? 'Fravælg alle' : 'Vælg alle';
    }
}

function pageLoaded() {

    transferFromSelect = document.getElementById('transferFrom');
    if(transferFromSelect !== null) {
        transferFromChoice = choiceService.initUserSelect('transferFrom', false);
        transferFromSelect.addEventListener('change', (event) => updateEntityListForTransferFrom(event.detail.value));
    }
    transferToSelect = document.getElementById('transferTo');
    if(transferToSelect !== null) {
        transferToChoice = choiceService.initUserSelect('transferTo', false);
    }

    transferResponsibilityModal = document.getElementById('transferResponsibilityModal');
    transferResponsibilityModal?.addEventListener('hidden.bs.modal', function () {
        transferFromChoice.removeActiveItems();
        transferToChoice.removeActiveItems();
        transferFromChoice.enable();
    });

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
                    const transferButton = `<button type="button" title="Overfør ansvar" class="btn btn-icon btn-xs me-1 modalInitButton" data-name="${name}" data-uuid="${uuid}"><i class="ti-angle-double-right fs-5"></i></button>`;
                    return gridjs.html(transferButton);
                }
            }
        ],
        data: data,
        language: {
            'noRecordsFound': "Ingen data fundet",
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

    initSaveAsExcelButtonClientside('inactiveUsersDatatable', 'user', 'users/inactive', 'Inaktive_ansvarlige', () => {
        // data variable is available from Thymeleaf
        return data.map(item => ({
            id: item.uuid,
            name: item.name
        }));
    });
}

function buttonHandler() {
    document.addEventListener('click', (event) => {
        if (event.target.closest('.toggleAllEntities')) {
            const total = document.querySelectorAll('#entityTable .entity-checkbox').length;
            const checked = document.querySelectorAll('#entityTable .entity-checkbox:checked').length;
            setAllEntitiesChecked(!(checked === total && total > 0));
        }

        if (event.target.closest('.transferButton')) {
            transferResponsibility();
        }

        const modalInitButton = event.target.closest('.modalInitButton');
        if (modalInitButton) {
            initModalWithDefaultTransferFrom(modalInitButton);
        }
    });

    document.addEventListener('change', (event) => {
        if (event.target.classList.contains('entity-checkbox')) {
            updateSelectedEntityCount();
        }
    });
}