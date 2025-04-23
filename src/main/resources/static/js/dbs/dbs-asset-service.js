
let dbsAssetService = new DBSAssetService();
document.addEventListener("DOMContentLoaded", function(event) {
    dbsAssetService.init();
});
function DBSAssetService() {

    this.init = () => {
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };

        var tableDiv = document.getElementById("assetsDatatable");
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
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "DBS Navn",
                    searchable: {
                        searchKey: 'name'
                    },
                },
                {
                    name: "Aktiv(er)",
                    sort: false,
                    searchable: {
                        searchKey: 'assetNames'
                    },
                    formatter: (cell, row) => {
                        const dbsAssetId = row.cells[0]['data'];

                        var options = '';
                        for (let index = 0; index < cell.length; ++index) {
                            const asset = cell[index];
                            options += `<option value="${asset.id}" selected>${asset.name}</option>`;
                        }

                        return gridjs.html(
                            `<select class="form-control form-select choices__input" data-assetid="${dbsAssetId}" name="assets" id="assetsSelect${dbsAssetId}" hidden="" tabindex="-1" multiple="multiple">` +
                            options +
                            `</select>`);
                    },
                    width: '300px'
                },
                {
                    name: "Sidst hentet",
                    searchable: {
                        searchKey: 'lastSync'
                    }
                },
                {
                    name: "Leverandør",
                    searchable: {
                        searchKey: 'supplier'
                    }
                }
            ],
            server:{
                url: gridDBSAssetsUrl,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(asset =>
                    [ asset.id, asset.name, asset.assets, asset.lastSync, asset.supplier ],
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
        let self = this;
        // set state on grid
        grid.on('ready', function() {
            if (!document.getElementsByClassName("gridjs-currentPage")[0]) {
                document.getElementsByClassName("gridjs-pages")[0].children[1].click();
            }

            // Initialize all Asset Choices.js
            Array.from(document.querySelectorAll("[id^='assetsSelect']")).map(select => select.id).forEach(elementId => {
                let assetChoices = choiceService.initAssetSelect(elementId, false);

                assetChoices.passedElement.element.addEventListener('removeItem', self.handleAddRemoveEvent, false);
                assetChoices.passedElement.element.addEventListener('addItem', self.handleAddRemoveEvent, false);
            });
        });


        new CustomGridFunctions(grid, gridDBSAssetsUrl, 'assetsDatatable')

        gridOptions.init(grid, document.getElementById("gridOptions"));

        // const grid = new gridjs.Grid(assetGridConfig).render( document.getElementById( "assetsDatatable" ));
    }


    this.handleAddRemoveEvent = async function (event) {
        const response = await fetch(gridDBSAssetsUpdateUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': token},
            body: JSON.stringify({
                id: event.target.dataset.assetid,
                assets: Array.from(event.target.selectedOptions).map(op => op.value)
            }),
        }).then(defaultResponseHandler)
            .catch(defaultErrorHandler);
    }

    this.updateUrl = (prev, query) => {
        return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
    };


}
