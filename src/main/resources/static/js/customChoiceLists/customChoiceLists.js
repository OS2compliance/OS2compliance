

class CustomChoiceListService {
    #tableIdentifier = "customChoiceListTable"
    #defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    constructor() {
        this.initGrid()
    }


    initGrid() {
        let gridConfig = {
            className: this.#defaultClassName,
            resizable: true,
            sort: true,
            pagination:true,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Titel"
                },
                {
                    id: 'handlinger',
                    name: 'Handlinger',
                    sort: 0,
                    width: '120px',
                    formatter: (cell, row) => {
                        if(isSuperuser) {
                            const id = row.cells[0]['data'];
                            return gridjs.html(`<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="onDeleteChoiceList(${id})"><i class="pli-trash fs-5"></i></button>`);
                        }
                    }
                },
            ],
            data:data,
            language: {
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

        const grid = new gridjs.Grid(gridConfig).render( document.getElementById( this.#tableIdentifier ));
    }



    onDeleteChoiceList(id) {}
}
