const incidentGridService = new IncidentGridService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentGridService.init();
});
function IncidentGridService() {
    this.filterFrom = '';
    this.filterTo = '';

    this.init = () => {
        var rerender = false;
        let fromPicker = initDatepicker('#filterFromBtn', '#filterFrom' );
        let filterFrom = localStorage.getItem("incidentFilterFrom");
        if (filterFrom != null && filterFrom !== "null") {
            fromPicker.setFullDate(new Date(filterFrom));
            this.filterFrom = fromPicker.getFormatedDate();
            rerender = true;
        }
        fromPicker.onSelect((date, formatedDate) => this.setFilterFrom(date, formatedDate));

        let toPicker = initDatepicker('#filterToBtn', '#filterTo' );
        let filterTo = localStorage.getItem("incidentFilterTo");
        if (filterTo != null && filterTo !== "null") {
            toPicker.setFullDate(new Date(filterTo));
            this.filterTo = toPicker.getFormatedDate();
            rerender = true;
        }
        toPicker.onSelect((date, formatedDate) => this.setFilterTo(date, formatedDate));
        incidentService.fetchColumnName()
            .then(columnNames => {
                this.initGrid(columnNames);
                if (rerender) {
                    this.incidentGrid.forceRender();
                }
            });
    }

    this.generateExcel = () => {
        window.location.href = `/reports/incidents/excel?from=${this.filterFrom}&to=${this.filterTo}`;
    }

    this.generateReport = () => {
        fetch(`/reports/incidents?from=${this.filterFrom}&to=${this.filterTo}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`${response.status} ${response.statusText}`);
                }
                response.text()
                    .then(data => {
                        var win = window.open("", "Print Rapport", "height=600,width=800");
                        win.document.write(data);
                        win.print();
                    });
            })
            .catch(error => toastService.error(error));
    }

    this.updateUrl = (prev, query) => {
        return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
    };

    this.setFilterFrom = (date, formattedDate) => {
        if (formattedDate == null) {
            formattedDate = '';
        }
        this.filterFrom = formattedDate;
        this.incidentGrid.updateConfig(this.currentConfig).forceRender();
        localStorage.setItem("incidentFilterFrom", date);
    }

    this.setFilterTo = (date, formattedDate) => {
        if (formattedDate == null) {
            formattedDate = '';
        }
        this.filterTo = formattedDate;
        this.incidentGrid.updateConfig(this.currentConfig).forceRender();
        localStorage.setItem("incidentFilterTo", date);
    }

    this.initGrid = (columnNames) => {
        let self = this;
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };
        this.buildColumns(columnNames);
        this.currentConfig = {
            className: defaultClassName,
            pagination: {
                limit: 50,
                server: {
                    url: (prev, page, size) => this.updateUrl(prev, `size=${size}&page=${page}&fromDate=${this.filterFrom}&toDate=${this.filterTo}`)
                }
            },
            search: {
                keyword: searchService.getSavedSearch(),
                server: {
                    url: (prev, keyword) => this.updateUrl(prev, `search=${keyword}&fromDate=${this.filterFrom}&toDate=${this.filterTo}`)
                },
                debounceTimeout: 1000
            },
            sort: {
                enabled: true,
                multiColumn: false,
                server: {
                    url: (prev, columns) => {
                        if (!columns.length) return prev;
                        const columnIds = this.columns.map(c => c.id);
                        const col = columns[0]; // multiColumn false
                        const order = columnIds[col.index];
                        return this.updateUrl(prev, 'dir=' + (col.direction === 1 ? 'asc' : 'desc') + ( order ? '&order=' + order : ''));
                    }
                }
            },
            columns: this.columns,
            server:{
                url: restUrl + 'list',
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => {
                    this.data = data;
                    return data.content.map(field => this.mapRow(field));
                },
                total: data => data.totalCount
            }
        };
        this.incidentGrid = new gridjs.Grid(this.currentConfig);
        this.incidentGrid.render(document.getElementById("incidentsTable"));
        searchService.initSearch(this.incidentGrid, this.currentConfig);
        const customGridFunctions = new CustomGridFunctions(this.incidentGrid, restUrl + 'list', incidentsTable);

        gridOptions.init(this.incidentGrid, document.getElementById("gridOptions"));

        initGridActions()

        initSaveAsExcelButton(customGridFunctions, 'Hændelseslog')
    }

    this.mapRow = (field) => {
        let columnValues = [];
        let customColumns = this.columns.filter((c) =>
            c.id !== 'id' && c.id !== 'title' && c.id !== 'createdAt' && c.id !== 'updatedAt' && c.id !== 'allowedActions');
        customColumns.forEach(c => {
            let added = false;
            field.responses.forEach(response => {
                if (c.id === response.indexColumnName) {
                    columnValues.push(response.answerValue);
                    added = true;
                }
            });
            if (!added) {
                columnValues.push("");
            }
        });
        return [field.id, field.name, field.createdAt, ...columnValues, field.updatedAt, field.allowedActions];
    }

    this.buildColumns = (columnNames) => {
        const columns = [
            {
                id: "id",
                hidden: true
            },
            {
                id: "title",
                name: "Titel",
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                },
                width: '250px'
            },
            {
                id: "createdAt",
                name: "Oprettet",
                width: '120px'
            }
        ]

        columnNames.forEach(c => {
            columns.push({
                id: c,
                name: c,
                sort: 0
            })
        });
        columns.push(
            {
                id: "updatedAt",
                name: "Opdateret"
            }
        )
        columns.push(
            {
                id: "allowedActions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    const name = row.cells[1]['data'].replaceAll("'", "\\'");
                    const attributeMap = new Map();
                    attributeMap.set('identifier', identifier);
                    attributeMap.set('name', name);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                }
            }
        );

        this.columns = columns;
    }
}

function initGridActions() {
    delegateListItemActions('incidentsTable',
        (id, elem) => incidentService.editIncident('editIncidentDialog', id),
        (id, name, elem) => incidentService.deleteIncident(incidentGridService.incidentGrid, id, name),
        )
}
