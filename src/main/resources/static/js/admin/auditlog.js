const csrfToken = document.getElementsByName("_csrf")[0].getAttribute("content");

// Variables
const searchRestUrl = "/rest/admin/log/auditlog/list";
const historyRestUrl = "/rest/admin/log/auditlog/history";
let historyModal;
let historyModalBody;

document.addEventListener('DOMContentLoaded', function () {
    init();
});

function init() {
    historyModal = document.getElementById('auditHistoryModal');
    historyModalBody = document.getElementById('auditHistoryModalBody');
    initGrid();
    buttonHandler();
}

function initGrid() {
    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    const grid = new gridjs.Grid({
        className: defaultClassName,
        sort: {
            enabled: true,
            multiColumn: false
        },
        columns: [
            {
                id: "performerName",
                name: "Brugernavn"
            },
            {
                id: "createdTimestamp",
                name: "Tidspunkt",
                formatter: (cell) => dateFormatter(cell)
            },
            {
                id: "entityTypeLabel",
                name: "Entitetstype"
            },
            {
                id: "entityName",
                name: "Entitetsnavn"
            },
            {
                id: "description",
                name: "Beskrivelse"
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: false,
                formatter: (cell, row) => {
                    const entityType = row.cells[6] ? row.cells[6]['data'] : null;
                    const entityId = row.cells[7] ? row.cells[7]['data'] : null;
                    const description = row.cells[4] ? row.cells[4]['data'] : null;
                    if (!entityType || !entityId || !isUpdateDescription(description)) {
                        return "";
                    }
                    return gridjs.html(`<button type="button" title="Vis historik" class="btn btn-icon btn-xs showAuditHistoryButton" data-entity-type="${entityType}" data-entity-id="${entityId}"><i class="ti-eye fs-5"></i></button>`);
                }
            },
            {
                id: "entityType",
                name: "entityType",
                hidden: true
            },
            {
                id: "entityId",
                name: "entityId",
                hidden: true
            }
        ],
        server: {
            url: searchRestUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': csrfToken
            },
            then: data => data.content.map(row => [
                row.performerName,
                row.createdTimestamp,
                row.entityTypeLabel,
                row.entityName,
                row.description,
                null,
                row.entityType,
                row.entityId
            ]),
            total: data => data.totalCount
        },
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
    }).render(document.getElementById("auditlogTable"));

    new CustomGridFunctions(grid, searchRestUrl, "auditlogTable", {
        sortDirection: 'DESC',
        sortColumn: 'createdTimestamp'
    });
}

function isUpdateDescription(description) {
    return typeof description === 'string' && description.includes(' opdaterede ');
}

function dateFormatter(rawDate) {
    const date = new Date(rawDate);
    if (date instanceof Date && !isNaN(date)) {
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${day}/${month}-${year} ${hours}:${minutes}:${seconds}`;
    }
    return "";
}

const isoDateTimePattern = /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}(:\d{2}(\.\d+)?)?)?$/;

function formatDiffValue(value) {
    if (value == null) {
        return '';
    }
    if (isoDateTimePattern.test(value)) {
        return dateFormatter(value);
    }
    return value;
}

async function showAuditHistory(entityType, entityId) {
    try {
        const params = new URLSearchParams({entityType, entityId});
        const url = `${historyRestUrl}?${params.toString()}`;
        const response = await fetch(url, {
            method: "GET",
            headers: {
                'X-CSRF-TOKEN': csrfToken,
                "Content-Type": "application/json"
            }
        })

        const diffs = await response.json();
        renderHistoryModal(diffs);
        new bootstrap.Modal(historyModal).show();
    } catch {
        renderHistoryModal([]);
        new bootstrap.Modal(historyModal).show();
    }
}

function renderHistoryModal(diffs) {
    if (!diffs || diffs.length === 0) {
        historyModalBody.innerHTML = '<p>Ingen tidligere ændringer fundet.</p>';
        return;
    }

    let rows = diffs.map(diff => `
        <tr>
            <td>${diff.field}</td>
            <td>${formatDiffValue(diff.oldValue)}</td>
            <td>${formatDiffValue(diff.newValue)}</td>
        </tr>
    `).join('');

    historyModalBody.innerHTML = `
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Felt</th>
                    <th>Fra</th>
                    <th>Til</th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

function buttonHandler() {
    document.addEventListener("click", function(event) {
        if (event.target.classList.contains("showAuditHistoryButton")) {
            const button = event.target;
            const entityId = button.dataset.entityId;
            const entityType = button.dataset.entityType;
            showAuditHistory(entityType, entityId);
        }
    });
}
