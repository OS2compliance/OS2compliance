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
        this.buildColumns(columnNames);
        this.incidentGrid = new gridjs.Grid({
            className: defaultClassName,
            pagination: {
                limit: 50,
                server: {
                    url: (prev, page, size) => this.updateUrl(prev, `size=${size}&page=${page}`)
                }
            },
            sort: false,
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
    }

    this.mapRow = (field) => {
        let columnValues = [];
        this.columns.forEach(c => {
            field.responses.forEach(response => {
                if (c.id === response.indexColumnName) {
                    columnValues.push(response.answerValue);
                }
            });
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
                name: "Titel"
            },
            {
                id: "createdAt",
                name: "Oprettet"
            }];
        columnNames.forEach(c => {
            this.columns.push({
                id: c,
                name: c
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

