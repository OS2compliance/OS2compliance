document.addEventListener("DOMContentLoaded", function (event) {
    initTable()
    initListItemActions()
    initCreateStandardButton()
    const success = document.body.dataset.success;

    if (success) {
        toastService.info(success);
    }

    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    [...tooltipTriggerList].forEach(el => new bootstrap.Tooltip(el));
});

function initCreateStandardButton() {
    const button = document.getElementById("createStandardButton");
    button?.addEventListener("click", () => createStandardService.openStandardModal(null, false))
}

function initListItemActions() {
    delegateListItemActions('tablePlaceholder',
        (identifier) => createStandardService.openStandardModal(`${identifier}`, true),
        (identifier, name) =>  createStandardService.openDeleteSwal(`${identifier}`, true)
        )
}

function initTable() {
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
                id: "identifier",
                name: "identifier",
                hidden: true
            },
            {
                id: "name",
                name: "Standard",
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                id: "compliance",
                name: "Efterlevelse"
            },
            {
                id: "progressBarValues",
                name: "Progression",
                formatter: (cell, row) => {
                    const statusCounts = row.cells[3]['data'];
                    const total = row.cells[4]['data'];

                    if (!total || total === 0) {
                        return gridjs.html(
                            '<div class="progress"><div class="progress-bar bg-secondary" style="width: 100%">Ingen data</div></div>'
                        );
                    }

                    // Map statuses to Bootstrap colors
                    const statusColors = {
                        READY: "bg-green-500",
                        IN_PROGRESS: "bg-blue-500",
                        NOT_STARTED: "bg-yellow-500",
                        NOT_RELEVANT: "bg-gray-500"
                    };

                    const statusTranslations = {
                        READY: "Færdig",
                        IN_PROGRESS: "I gang",
                        NOT_STARTED: "Ikke startet",
                        NOT_RELEVANT: "Ikke relevant"
                    }

                    let bars = "";
                    for (const [status, count] of Object.entries(statusCounts)) {
                        if (count > 0) {
                            const percent = (count / total) * 100;
                            bars += `
                                <div class="progress-bar ${statusColors[status] || ''}" 
                                     role="progressbar" 
                                     style="width: ${percent.toFixed(2)}%" 
                                     aria-valuenow="${count}" 
                                     aria-valuemin="0" 
                                     aria-valuemax="${total}"
                                     data-bs-toggle="dropdown" 
                                     aria-expanded="false"
                                     title="${statusTranslations[status] || ''}: ${count}">
                                </div>`;
                        }
                    }
                    return gridjs.html(`<div class="progress">${bars}</div>`);
                }
            },
            {
              id: "totalCount",
              hidden: true
            },
            {
                id: "allowedActions",
                name: "Handlinger",
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    const attributeMap = new Map();
                    attributeMap.set('identifier', identifier);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
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
    }).render(document.getElementById("tablePlaceholder"));
}