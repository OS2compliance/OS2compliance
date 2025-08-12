document.addEventListener("DOMContentLoaded", function (event) {
    initTable()
    initListItemActions()
});

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
                id: "allowedActions",
                name: "Handlinger",
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    formatAllowedActions(cell, row, identifier);
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