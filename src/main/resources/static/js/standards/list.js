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
    const cache = new Map();
    let standardsGrid = new gridjs.Grid({
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
                attributes: (cell, row) => {
                    if (!row) return {};
                    return {'data-extra-id': row.cells[0]['data']};
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
    });
    standardsGrid.render(document.getElementById("tablePlaceholder"));
    standardsGrid.on('ready', () => {
        document.querySelectorAll('[data-extra-id]').forEach(async td => {
            const id = td.getAttribute('data-extra-id');
            if (td.dataset.loaded) return; // undgå duplikat ved re-renders
            td.dataset.loaded = '1';
            if (cache.has(id)) { td.innerHTML = cache.get(id); return; }

            let html = await fetch(`${viewUrl}${id}/progress`).then(r => r.text());
            html = `<div class="progress flex-grow-1" style="height: 1.2rem;">${html}</div>`
            cache.set(id, html);
            td.innerHTML = html;
        });
    });
}