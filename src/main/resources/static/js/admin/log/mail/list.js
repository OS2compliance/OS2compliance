document.addEventListener("DOMContentLoaded", function (event) {

});

class MailLogListService {
    #searchRestUrl = "" // TODO
    #columnPropertyNames = [
        'sentAt',
        'receiver',
        'templateType',
        'subject'
    ]
    #tableIdentifier = "customChoiceListTable"
    #csrfToken = document.getElementsByName("_csrf")[0].getAttribute("content");
    #gridConfig = {
        className: {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        },
        columns: [
            {
                name: "Sendt",
                searchable: {
                    searchKey: 'sentAt'
                },
                formatter: (cell, row) => {
                    // TODO - format to nice display date time
                }
            },
            {
                name: "Modtager",
                searchable: {
                    searchKey: 'receiver'
                }
            },
            {
                name: "Type",
                searchable: {
                    searchKey: 'templateType',
                    fieldId :'' // TODO - enum for types?
                }
            },
            {
                name: "Subject",
                searchable: {
                    searchKey: 'subject'
                }
            }
        ],
        server:{
            url: this.#searchRestUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': this.#csrfToken
            },
            then: data => data.content.map(obj => {
                    const result = []
                    for (const property of this.#columnPropertyNames) {
                        result.push(obj[property])
                    }
                    return result;
                }
            ),
            total: data => data.totalCount
        },
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
    }

    initGrid() {
        const grid = new gridjs.Grid(this.#gridConfig)
            .render( document.getElementById( this.#tableIdentifier ));

        new CustomGridFunctions(grid,  this.#searchRestUrl,  this.#tableIdentifier)
    }
}