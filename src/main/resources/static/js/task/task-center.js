let today = new Date();

const DateDiff = {
    inDays: function (d1, d2) {
        const t2 = d2.getTime();
        const t1 = d1.getTime();
        return Math.floor((t2 - t1) / (24 * 3600 * 1000));
    }
};

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

document.addEventListener("DOMContentLoaded", function() {

    initGrid()

});


function deleteClicked(taskId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra TASK slettes også.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteUrl}${taskId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
}

function initGrid() {
    let gridConfig = {
        className: defaultClassName,
        columns: [
            {
                name: "id",
                hidden: true
            },
            {
                name: "Opgavenavn",
                searchable: {
                    searchKey: 'name',
                },
                formatter: (cell, row) => {
                    const url = viewUrl + row.cells[0]['data'];
                    return gridjs.html(`<a href="${url}">${cell}</a>`);
                }
            },
            {
                name: "Opgave type",
                searchable: {
                    searchKey: 'taskType',
                    fieldId: "assignmentTypeSelector"
                },
            },
            {
                name: "Ansvarlig",
                searchable: {
                    searchKey: 'responsibleUser.name',
                },
            },
            {
                name: "Afdeling",
                searchable: {
                    searchKey: 'responsibleOU.name',
                },
            },
            {
                name: "Tags",
                searchable: {
                    searchKey: 'tags',
                },
                formatter: (cell, row) => {
                    let result = '';
                    if (cell != null && cell.trim() !== '') {
                        let tags = cell.split(',');
                        for (let i =0; i< tags.length; i++) {
                            result += '<div class=" badge bg-info mb-1">'+tags[i]+'</div>';
                        }
                    }
                    return gridjs.html(result, 'div')
                },
            },
            {
                name: "Deadline",
                searchable: {
                    searchKey: 'nextDeadline',
                },
                width: '90px',
                formatter: (cell, row) => {
                    var completed = row.cells[9]['data'];
                    var type = row.cells[2]['data'];
                    if (completed && type === "Opgave") {
                        return gridjs.html(`<span>${cell}</span>`);
                    }

                    var dateString = cell.replace(" ", "/");
                    dateString = dateString.replace("-", "/");
                    var dateSplit = dateString.split("/");
                    var cellDate = new Date(dateSplit[2] + "-" + dateSplit[1] + "-" + dateSplit[0] + "T23:59:59");
                    var diff = DateDiff.inDays(today, cellDate);

                    if (diff < 0) {
                        return gridjs.html(`<span style="color: red;">${cell}</span>`);
                    }
                    else if (diff < 31 && diff >= 0 ) {
                        return gridjs.html(`<span style="color: orange;">${cell}</span>`)
                    }
                    else {
                        return gridjs.html(`<span>${cell}</span>`);
                    }
                }
            },
            {
                name: "Gentages",
                searchable: {
                    searchKey: 'taskRepetition',
                    fieldId:'taskRepetitionSelector'
                },
                width: '95px'
            },
            {
                name: "Resultat",
                searchable: {
                    searchKey: 'taskResult',
                },
                hidden: true,
            },
            {
                name: "Status",
                searchable: {
                    sortKey: 'completed'
                },
                width: '100px',
                formatter: (cell, row) => {
                    let status = "";
                    let type = row.cells[2]['data'];

                    // if completed and task type opgave
                    if (cell && type === "Opgave") {
                        status = '<div class="d-block badge bg-success">Udført</div>'
                    } else {
                        let deadline = row.cells[6]['data'];
                        let dateString = deadline.replace(" ", "/");
                        dateString = dateString.replace("-", "/");
                        let dateSplit = dateString.split("/");
                        let deadlineAsDate = new Date(dateSplit[2] + "-" + dateSplit[1] + "-" + dateSplit[0] + "T23:59:59");
                        let diff = DateDiff.inDays(today, deadlineAsDate);
                        let statusText = 'Ikke udført';
                        if(row.cells[8]['data'] === 'NO_ERROR') {
                            statusText = 'Ingen fejl';
                        } else if (row.cells[8]['data'] === 'NO_CRITICAL_ERROR') {
                            statusText = 'Ingen kritiske fejl';
                        } else if (row.cells[8]['data'] === 'CRITICAL_ERROR') {
                            statusText = 'Kritiske fejl';
                        }

                        if (diff < 0) {
                            status = `<div class="d-block badge bg-danger">${statusText}</div>`;
                        } else if (diff < 31 && diff >= 0) {
                            status = `<div class="d-block badge bg-warning">${statusText}</div>`;
                        } else {
                            status = `<div class="d-block badge bg-gray-800">${statusText}</div>`;
                        }
                    }

                    return gridjs.html(status, 'div')
                },
            },
            {
                id: 'allowedActions',
                name: 'Handlinger',
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
        ],
        server: {
            url: gridTasksUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(task =>
                [ task.id, task.name, task.taskType,
                    task.responsibleUser, task.responsibleOU, task.tags, task.nextDeadline,
                    task.taskRepetition !== null ? task.taskRepetition : "", task.taskResult, task.completed, task.allowedActions ]
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
    };
    const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "tasksDatatable" ));

    //Enables custom column search, serverside sorting and pagination
    const customGridFunctions = new CustomGridFunctions(grid, gridTasksUrl, exportTasksUrl, 'tasksDatatable');

    gridOptions.init(grid, document.getElementById("gridOptions"));

    initGridActions()

    initSaveAsExcelButton(customGridFunctions, 'Opgavecenter')
}

function initGridActions() {
    delegateListItemActions('tasksDatatable',
        (id, elem) => editTaskService.showEditDialog(id),
        (id, name, elem) => deleteClicked(id, name),
        (id, elem) =>copyTaskService.showCopyDialog(id) ,
    )
}