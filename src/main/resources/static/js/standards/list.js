document.addEventListener("DOMContentLoaded", function (event) {
    initTable()
    initListItemActions()
});

function initListItemActions() {
    const table = document.getElementById("tablePlaceholder");
    initEditListItemButtons(table);
    initDeleteListItemButtons(table);
}

function initEditListItemButtons(table) {
    table.addEventListener("click", (e) => {
        const target = e.target;
        if (target.classList.contains('editBtn')) {
            const identifier = target?.dataset.identifier;
            createStandardService.openStandardModal(`${identifier}`, true)
        }
    })
}

function initDeleteListItemButtons(table) {
    table.addEventListener("click", (e) => {
        const target = e.target;
        if (target.classList.contains('deleteBtn')) {
            const identifier = target?.dataset.identifier;
            createStandardService.openDeleteSwal(`${identifier}`, true)
        }
    })
}

function initTable() {
    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    const editTemplate = document.getElementById('editListItemButtonTemplate')
    const deleteTemplate = document.getElementById('deleteListItemButtonTemplate')

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
                    const container = document.createElement('span');
                    const identifier = row.cells[0]['data'];

                    if (cell.includes('editable')) {
                        const buttonFragment = editTemplate.content.cloneNode(true);
                        const button = buttonFragment.firstElementChild;
                        button.dataset.identifier = identifier;
                        container.appendChild(button);
                    }
                    if (cell.includes('deletable')) {
                        const buttonFragment = deleteTemplate.content.cloneNode(true);
                        const button = buttonFragment.firstElementChild;
                        button.dataset.identifier = identifier;
                        container.appendChild(button);
                    }

                    return gridjs.html(container.innerHTML); // Ugly hack because grid.js sucks
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