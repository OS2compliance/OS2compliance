class CustomChoiceListService {
    #tableIdentifier = "customChoiceListTable"
    #defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };
    #modal

    constructor() {
        this.initGrid()
        initSaveAsExcelButtonWithDefaultGrid('customChoiceListTable', 'Valglister');
    }

    initGrid() {
        let gridConfig = {
            className: this.#defaultClassName,
            resizable: true,
            sort: true,
            pagination: true,
            autoWidth: true,
            columns: [
                {
                    id: "id",
                    name: "id",
                    hidden: true
                },
                {
                    id: "name",
                    name: "Titel",
                    formatter: (cell, row) => {
                        let url = choiceListViewUrl + row.cells[0]['data'];
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
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
                    'results': 'Valglister',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };

        const grid = new gridjs.Grid(gridConfig).render(document.getElementById(this.#tableIdentifier));
    }
}

