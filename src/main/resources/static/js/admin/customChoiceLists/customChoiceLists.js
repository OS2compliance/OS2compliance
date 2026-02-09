import { initSaveAsExcelButtonClientside } from "/js/excel-export/excel-export-init.js";

let token = document.getElementsByName("_csrf")[0].getAttribute("content");
let customChoiceListService;

document.addEventListener("DOMContentLoaded", function(event) {
    customChoiceListService = new CustomChoiceListService()
});

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

        initSaveAsExcelButtonClientside('customChoiceListTable', 'choiceList', 'choicelists/custom', 'Valglister', () => {
            return data.map(item => ({
                id: String(item.id),
                name: item.name
            }));
        });
    }
}

