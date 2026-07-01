document.addEventListener('DOMContentLoaded', function () {
    init();
});

function init() {
    initGrid();
}

function initGrid() {
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
                id: "responsibleName",
                name: "Brugernavn"
            },
            {
                id: "timestamp",
                name: "Tidspunkt"
            },
            {
                id: "action",
                name: "Handling"
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
                name: "Handlinger"
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
    }).render(document.getElementById("auditlogTable"));
}