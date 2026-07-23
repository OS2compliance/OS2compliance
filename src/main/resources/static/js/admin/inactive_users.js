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
        .map(checkbox => Number(checkbox.dataset.id));

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

// Renders one collapsible group per entity type, with one checkbox per responsibility. All checked by default.
function buildEntityList(items) {
    const container = document.getElementById('entityTable');
    container.replaceChildren();

    if (!items.length) {
        const empty = document.createElement('p');
        empty.className = 'text-muted mb-0';
        empty.textContent = 'Brugeren er ikke ansvarlig for noget';
        container.appendChild(empty);
        updateSelectedEntityCount();
        return;
    }

    const groups = new Map();
    items.forEach(item => {
        if (!groups.has(item.type)) {
            groups.set(item.type, []);
        }
        groups.get(item.type).push(item);
    });

    let groupIndex = 0;
    groups.forEach((groupItems) => {
        groupIndex++;
        const groupId = `entityGroup${groupIndex}`;

        const groupWrapper = document.createElement('div');
        groupWrapper.className = 'mb-2';

        const groupToggle = document.createElement('a');
        groupToggle.className = 'd-block fw-bold text-decoration-none';
        groupToggle.href = `#${groupId}`;
        groupToggle.dataset.bsToggle = 'collapse';
        groupToggle.setAttribute('role', 'button');
        groupToggle.setAttribute('aria-expanded', 'true');
        groupToggle.setAttribute('aria-controls', groupId);
        groupToggle.textContent = `${groupItems[0].typeMessage} (${groupItems.length})`;
        groupWrapper.appendChild(groupToggle);

        const groupBody = document.createElement('div');
        groupBody.className = 'collapse show ps-3';
        groupBody.id = groupId;

        groupItems.forEach(item => {
            const checkboxId = `entity-${item.type}-${item.id}`;

            const formCheck = document.createElement('div');
            formCheck.className = 'form-check';

            const checkbox = document.createElement('input');
            checkbox.className = 'form-check-input entity-checkbox';
            checkbox.type = 'checkbox';
            checkbox.id = checkboxId;
            checkbox.dataset.id = item.id;
            checkbox.checked = true;

            const label = document.createElement('label');
            label.className = 'form-check-label';
            label.htmlFor = checkboxId;
            label.textContent = item.name;

            formCheck.append(checkbox, label);
            groupBody.appendChild(formCheck);
        });

        groupWrapper.appendChild(groupBody);
        container.appendChild(groupWrapper);
    });

    updateSelectedEntityCount();
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