const incidentGridService = new IncidentGridService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentGridService.init();
});
function IncidentGridService() {
    this.init = () => {
        incidentService.fetchColumnName()
            .then(columnNames => {
                this.initGrid(columnNames);
            });
    }

    this.updateUrl = (prev, query) => {
        return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
    };

    this.initGrid = (columnNames) => {
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };
        // load saved state assets
        const incidentsGridKey = window.location.pathname;
        const incidentsGridKeySearch = incidentsGridKey + '-incident-search'
        const savedSearchIncidents = localStorage.getItem(incidentsGridKeySearch);
        let initialIncidentLoadDone = false;  // flag to ensure initial load only happens once

        this.buildColumns(columnNames);
        this.incidentGrid = new gridjs.Grid({
            className: defaultClassName,
            pagination: {
                limit: 50,
                server: {
                    url: (prev, page, size) => this.updateUrl(prev, `size=${size}&page=${page}`)
                }
            },
            search: {
                keyword: savedSearchIncidents,
                server: {
                    url: (prev, keyword) => this.updateUrl(prev, `search=${keyword}`)
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
                then: data => { this.data = data;
                    return data.content.map(field => this.mapRow(field));
                },
                total: data => data.totalCount
            }
        });
        this.incidentGrid.render(document.getElementById("incidentsTable"));
        // set state on grid
        this.incidentGrid.on('ready', function() {
            // only apply saved state on the first load
            if (!initialIncidentLoadDone) {
                const searchInput = document.querySelector('#incidentsTable .gridjs-search-input');
                if (searchInput) {
                    searchInput.addEventListener('input', function() {
                        if (this.value === '') {
                            localStorage.removeItem(incidentsGridKeySearch);
                        } else {
                            localStorage.setItem(incidentsGridKeySearch, this.value);
                        }
                    });
                }

                initialIncidentLoadDone = true;  // ensure this only runs once
            }
        });
    }

    this.mapRow = (field) => {
        let columnValues = [];
        let customColumns = this.columns.filter((c) =>
            c.id !== 'id' && c.id !== 'name' && c.id !== 'createdAt' && c.id !== 'updatedAt' && c.id !== 'actions');
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
        return [field.id, field.name, field.createdAt, ...columnValues, field.updatedAt];
    }

    this.buildColumns = (columnNames) => {
        this.columns = [{
                id: "id",
                hidden: true
            },
            {
                id: "name",
                name: "Titel",
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                id: "createdAt",
                name: "Oprettet"
            }];
        columnNames.forEach(c => {
            this.columns.push({
                id: c,
                name: c,
                sort: 0
            })
        });
        this.columns.push(
            {
                id: "updatedAt",
                name: "Opdateret"
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const name = row.cells[1]['data'];
                    return gridjs.html(
                        `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentService.editIncident('editIncidentDialog', '${id}')"><i class="pli-pencil fs-5"></i></button>`
                        + `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentService.deleteIncident(incidentGridService.incidentGrid, '${id}', '${name}')"><i class="pli-trash fs-5"></i></button>`);
                }
            });
    }

}

