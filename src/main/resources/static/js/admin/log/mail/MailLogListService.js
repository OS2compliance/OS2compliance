export default class MailLogListService {
    /**Endpoint for grid data*/
    #searchRestUrl = "/rest/admin/log/mail/list";
    #exportRestUrl = "/rest/admin/log/mail/export";

    /**Array specifying the property names and order of properties in the received dto*/
    #columnPropertyNames = [
        'sentAt',
        'receiver',
        'type',
        'subject'
    ]
    /**ID of the containing element*/
    #tableIdentifier = "mailLogTable"
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
                formatter: (cell, row) => this.#dateFormatter(cell),
                sort: this.#sortByDate
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
                    searchKey: 'type',
                    fieldId: 'mailLogTemplateTypeSearchSelector'
                }
            },
            {
                name: "Subject",
                searchable: {
                    searchKey: 'subject'
                }
            }
        ],
        server: {
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
        },

    }

    /**
     * Initialized the grid
     */
    initGrid() {
        const grid = new gridjs.Grid(this.#gridConfig)
            .render(document.getElementById(this.#tableIdentifier));

        // Initialized search, pagination and so forth. Mutates specific parts of table config
        const customGridFunctions = new CustomGridFunctions(grid, this.#searchRestUrl, this.#exportRestUrl, this.#tableIdentifier, {
            sortDirection: 'DESC',
            sortField: 'sentAt',
        })

        initSaveAsExcelButton(customGridFunctions, 'Mail_Logs');
    }

    #dateFormatter(rawDate) {
        const date = new Date(rawDate)
        if (date instanceof Date && !isNaN(date)) {
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0'); // Month is 0-indexed
            const year = date.getFullYear();
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');

            return `${day}/${month}-${year} ${hours}:${minutes}:${seconds}`;
        }
        return ""
    }

    /**
     * Returns sort configuration for Date sorting
     * @returns {{compare: (function(*, *): boolean)}}
     */
    #sortByDate() {
        return {
            enabled: true,
            direction: 'desc',
            compare: (a, b) => {
                return new Date(b) - new Date(a);
            }
        }
    }
}