
let dbsOversightService = new DBSOversightService();
document.addEventListener("DOMContentLoaded", function(event) {
    dbsOversightService.init();
});
function DBSOversightService() {

    this.init = () => {
        let self = this;

        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };
        const tableDiv = document.getElementById("assetsDatatable");

        let gridConfig = {
            className: defaultClassName,
            search: {
                enabled: true,
                keyword: searchService.getSavedSearch(),
                server: {
                    url: (prev, keyword) => this.updateUrl(prev, `search=${keyword}`)
                },
                debounceTimeout: 1000
            },
            pagination: {
                limit: 25,
                resetPageOnUpdate: false,
                server: {
                    url: (prev, page, size) => this.updateUrl(prev, `size=${size}&page=${page}`)
                }
            },
            sort: {
                enabled: true,
                multiColumn: false,
                server: {
                    url: (prev, columns) => {
                        if (!columns.length) return prev;
                        const columnIds = ['id', 'supplierId', 'outstandingId', 'name', 'supplier', 'supervisoryModel', 'dbsAssets', 'oversightResponsible', 'lastInspection', 'lastInspectionStatus', 'outstandingSince' ];
                        const col = columns[0]; // multiColumn false
                        const order = columnIds[col.index];
                        return this.updateUrl(prev, 'dir=' + (col.direction === 1 ? 'asc' : 'desc') + ( order ? '&order=' + order : ''));
                    }
                }
            },
            columns: [
                {
                    name: "id",
                    hidden: true,
                },
                {
                    name: "suplierId",
                    hidden: true,
                },
                {
                    name: "outstandingTaskId",
                    hidden: true,
                },
                {
                    name: "Navn",
                    formatter: (cell, row) => {
                        let assetId = row.cells[0].data;
                        let html = `<a href="${assetsUrl}${assetId}">${cell}</a>`
                        return gridjs.html(html);
                    }
                },
                {
                    name: "Leverandør",
                    formatter: (cell, row) => {
                        let supplierId = row.cells[1].data;
                        let html = `<a href="${suppliersUrl}${supplierId}">${cell}</a>`
                        return gridjs.html(html);
                    }
                },
                {
                    name: "Tilsynsform",
                },
                {
                    name: "DBS",
                    sort: false,
                    formatter: (cell, row) => {
                        var html = '<ul>'

                        for (let i = 0; i < cell.length; i++) {
                            html += '<li>' + cell[i].name + '</li>'
                        }

                        html += '</ul>'
                        return gridjs.html(html);
                    }
                },
                {
                    name: "Ansvarlig",
                    width: "250px",
                    formatter: (cell, row) => {
                        const assetId = row.cells[0]['data'];
                        const option = cell !== null ? `<option selected>${cell}</option>` : '';
                        const disabled = !superUser ? 'disabled="true"' : '';
                        return gridjs.html(
                            `<select class="form-control form-select choices__input" data-assetid="${assetId}" name="assets" id="responsibleSelect${assetId}" tabindex="-1" ${disabled}>` +
                            option +
                            `</select>`);
                    },
                },
                {
                    name: "Sidste tilsyn"
                },
                {
                    name: "Resultat",
                    width: "100px",
                    formatter: (cell, row) => {
                        let status = cell;
                        if (cell === "Grøn") {
                            status = [
                                '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Gul") {
                            status = [
                                '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Rød") {
                            status = [
                                '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>'
                            ]
                        }
                        return gridjs.html(status, 'div');
                    }
                },
                {
                    name: "Ubehandlet tilsyn",
                    formatter: (cell, row) => {
                        let taskId = row.cells[2].data;
                        if (taskId == null) {
                            return "";
                        }
                        let html = `<a href="${tasksUrl}${taskId}">${cell}</a>`
                        return gridjs.html(html);
                    }
                }
            ],
            server:{
                url: gridDBSOversightUrl,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(asset =>
                    [ asset.id, asset.supplierId, asset.outstandingId, asset.name, asset.supplier, asset.supervisoryModel, asset.dbsAssets, asset.oversightResponsible, asset.lastInspection, asset.lastInspectionStatus, asset.outstandingSince ],
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
                    'results': 'aktiver',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };
        const grid = new gridjs.Grid(gridConfig).render(tableDiv);
        // set state on grid
        grid.on('ready', function() {
            Array.from(document.querySelectorAll("[id^='responsibleSelect']")).map(select => select.id).forEach(elementId => {
                let userChoices = choiceService.initUserSelect(elementId, false);
                userChoices.passedElement.element.addEventListener('addItem', self.handleAddRemoveEvent, false);
                userChoices.passedElement.element.addEventListener('removeItem', self.handleAddRemoveEvent, false);
            });
            if (!document.getElementsByClassName("gridjs-currentPage")[0]) {
                document.getElementsByClassName("gridjs-pages")[0].children[1].click();
            }
        });
        searchService.initSearch(grid, gridConfig);
        gridOptions.init(grid, document.getElementById("gridOptions"));
    }

    this.handleAddRemoveEvent = async (event) => {
        const assetId = event.target.dataset.assetid;
        let selected = event.target.selectedOptions;
        if (selected.length === 0) {
            deleteData(`/rest/assets/${assetId}/oversightresponsible`)
                .then(defaultResponseHandler)
                .catch(defaultErrorHandler);
            return;
        }
        const userUuid = selected[0].value;
        putData(`/rest/assets/${assetId}/oversightresponsible?userUuid=${userUuid}`, {})
            .then(defaultResponseHandler)
            .catch(defaultErrorHandler);
    }

    this.updateUrl = (prev, query) => {
        return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
    };
}
