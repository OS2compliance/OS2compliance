const searchRestUrl = "/rest/admin/log/auditlog/list";
const historyRestUrl = "/rest/admin/log/auditlog/history";
const csrfToken = document.getElementsByName("_csrf")[0].getAttribute("content");

let historyModal, historyModalBody;

document.addEventListener('DOMContentLoaded', function () {
    init();
});

// Exposed for the inline onclick handler in the grid action button below.
// Required because this file is loaded as type="module", so top-level functions are not on window.
window.showAuditHistory = showAuditHistory;

function init() {
    historyModal = document.getElementById('auditHistoryModal');
    historyModalBody = document.getElementById('auditHistoryModalBody');
    initGrid();
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
                id: "entityType",
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
                    const entityType = row.cells[2]['data'];
                    const entityId = row.cells[6] ? row.cells[6]['data'] : null;
                    if (!entityType || !entityId) {
                        return "";
                    }
                    return gridjs.html(`<button type="button" title="Vis historik" class="btn btn-icon btn-xs" data-entity-type="${entityType}" data-entity-id="${entityId}" onclick="showAuditHistory(this)"><i class="ti-eye fs-5"></i></button>`);
                }
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
                row.entityType,
                row.entityName,
                row.description,
                null,
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

function showAuditHistory(elem) {
    const entityType = elem.dataset.entityType;
    const entityId = elem.dataset.entityId;

    const params = new URLSearchParams({ entityType, entityId });
    fetch(`${historyRestUrl}?${params.toString()}`, {
        headers: { 'X-CSRF-TOKEN': csrfToken }
    })
        .then(response => response.json())
        .then(diffs => {
            renderHistoryModal(diffs);
            new bootstrap.Modal(historyModal).show();
        })
        .catch(() => {
            renderHistoryModal([]);
            new bootstrap.Modal(historyModal).show();
        });
}

function renderHistoryModal(diffs) {
    if (!diffs || diffs.length === 0) {
        historyModalBody.innerHTML = '<p>Ingen tidligere ændringer fundet.</p>';
        return;
    }

    let rows = diffs.map(diff => `
        <tr>
            <td>${diff.field}</td>
            <td>${diff.oldValue ?? ''}</td>
            <td>${diff.newValue ?? ''}</td>
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
