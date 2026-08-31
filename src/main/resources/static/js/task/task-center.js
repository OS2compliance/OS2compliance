import {initStatisticView} from "../statistic/statisticView.js";
import ColumnOptions from "../grid-js-extension/column-options.js";
import formatTags from "../tags/tag-grid-formatter.js";
import { initSaveAsExcelButton } from "../excel-export/excel-export-init.js";
import {BadgeData, createBadges} from "../component/badge.js";
import { initYearWheel } from "./year-wheel.js";
import { DateRangeFilter } from "../component/date-range-filter.js";

let today = new Date();
let token = document.getElementsByName("_csrf")[0].getAttribute("content");
let grid = null;

document.addEventListener("DOMContentLoaded", function() {

    initGrid()

    initStatisticView('task')

    initYearWheel(yearWheelUrl, token);

});

const DateDiff = {
    inDays: function (d1, d2) {
        const t2 = d2.getTime();
        const t1 = d1.getTime();
        return Math.floor((t2 - t1) / (24 * 3600 * 1000));
    }
};

function escapeAttribute(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

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
                name: "Tilknytninger",
                searchable: {
                    searchKey: 'relatedEntities'
                },
                formatter: (cell) => {
                    if (!Array.isArray(cell)) {
                        return ""
                    }

                    const badgedata = cell.map(rel => new BadgeData(rel.name, rel.link, rel.helpText, rel.color))

                    const badges = createBadges(badgedata)
                    return gridjs.html(badges.outerHTML);
                },
            },
            {
                name: "Ansvarlig",
                searchable: {
                    searchKey: 'responsibleNames',
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
                    searchKey: 'tagNames',
                },
                formatter: (cell, row) => formatTags(cell, row),
            },
            {
                name: "Startdato",
                searchable: {
                    searchKey: 'startDate',
                },
                width: '90px',
            },
            {
                name: "Slutdato",
                searchable: {
                    searchKey: 'nextDeadline',
                },
                width: '90px',
                formatter: (cell, row) => {
                    if (!cell) {
                        return gridjs.html(`<span>-</span>`);
                    }

                    var completed = row.cells[12]['data'];
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
                name: "Sidst udført",
                searchable: {
                    searchKey: 'lastCompletionDate'
                },
                formatter: (cell, row) => {
                    if (!cell || cell.trim() === '') {
                        return gridjs.html(`<span>-</span>`);
                    }

                    var dateParts = cell.split('-');
                    if (dateParts.length === 3) {
                        var formattedDate = `${dateParts[2]}/${dateParts[1]}-${dateParts[0]}`;
                        return gridjs.html(`<span>${formattedDate}</span>`);
                    }

                    return gridjs.html(`<span>${cell}</span>`);
                }
            },
            {
                name: "Status",
                searchable: {
                    sortKey: 'completed',
                    searchKey: 'taskDeadlineStatus',
                    fieldId: "taskStatusSearchSelector"
                },
                formatter: (cell, row) => {
                    let status = '';
                    let type = row.cells[2]['data'];
                    let deadline = row.cells[8]['data'] || null;
                    let inProgress = row.cells[14]['data'];
                    let note = row.cells[15]['data'];
                    let completed = (cell && type === "Opgave") || row.cells[12]['data'] === true;

                    // completed always wins over in progress
                    if (completed) {
                        status = '<div class="d-block badge bg-success">Udført</div>'
                    } else if (inProgress === true) {
                        let noteAttribute = note ? ` title="${escapeAttribute(note)}"` : '';
                        status = `<div class="d-block badge bg-lightblue"${noteAttribute}>I gang</div>`
                    } else if (deadline) {
                        let dateString = deadline.replace(" ", "/");
                        dateString = dateString.replace("-", "/");
                        let dateSplit = dateString.split("/");

                        if (dateSplit.length >= 3) {
                            let deadlineAsDate = new Date(dateSplit[2] + "-" + dateSplit[1] + "-" + dateSplit[0] + "T23:59:59");
                            let today= new Date();
                            let statusText = 'Ikke udført';

                            if (deadlineAsDate < today) {
                                statusText = 'Overskredet';
                            }

                            const taskResult = row.cells[10]['data'];
                            if(taskResult === 'NO_ERROR') {
                                statusText = 'Ingen fejl';
                            } else if (taskResult === 'NO_CRITICAL_ERROR') {
                                statusText = 'Ingen kritiske fejl';
                            } else if (taskResult === 'CRITICAL_ERROR') {
                                statusText = 'Kritiske fejl';
                            }

                            let diff = DateDiff.inDays(today, deadlineAsDate);
                            if (diff < 0) {
                                status = `<div class="d-block badge bg-danger">${statusText}</div>`;
                            } else if (diff < 31 && diff >= 0) {
                                status = `<div class="d-block badge bg-warning">${statusText}</div>`;
                            } else {
                                status = `<div class="d-block badge bg-gray-800">${statusText}</div>`;
                            }
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
            },
            {
                name: "inProgress",
                hidden: true
            },
            {
                name: "note",
                hidden: true
            }
        ],
        server: {
            url: gridTasksUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(task =>
                [ task.id, task.name, task.taskType, task.relatedEntities,
                    task.responsibleNames, task.responsibleOU, task.tags, task.startDate, task.nextDeadline,
                    task.taskRepetition !== null ? task.taskRepetition : "", task.taskResult, task.lastCompletionDate, task.completed, task.allowedActions, task.inProgress, task.inProgressNote]
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
    const datatableId = 'tasksDatatable';
    grid = new gridjs.Grid(gridConfig).render( document.getElementById( datatableId ));

    //Enables custom column search, serverside sorting and pagination
    const customGridFunctions = new CustomGridFunctions(grid, gridTasksUrl, datatableId);

    initDateRangeFilter(customGridFunctions);

    new ColumnOptions(
        datatableId,
        grid,
        ['opgavenavn', 'allowedActions'],
        ['opgavenavn', 'allowedActions', 'opgaveType', 'ansvarlig', 'startdato', 'slutdato', 'status', 'resultat'],
        ['id', 'inProgress', 'note'])

    initGridActions()

    initSaveAsExcelButton(customGridFunctions, 'task', 'tasks', 'Opgavecenter')
}

function initDateRangeFilter(customGridFunctions) {
    const filterValue = (key) => customGridFunctions.state.searchValues[key] || '';
    const setFilter = (key, value) => {
        customGridFunctions.updateColumnValue(key, value || '');
        customGridFunctions.state.page = 0;
        customGridFunctions.saveState();
        customGridFunctions.onSearch();
    };

    new DateRangeFilter({
        containerEl: document.getElementById('taskDateRangeFilter'),
        getValue: () => ({ from: filterValue('fromDate'), to: filterValue('toDate') }),
        onApply: (from, to) => {
            setFilter('fromDate', from);
            setFilter('toDate', to);
        },
        onClear: () => {
            setFilter('fromDate', '');
            setFilter('toDate', '');
        }
    });

    const dateFieldSelect = document.getElementById('taskDateFieldSelect');
    dateFieldSelect.value = filterValue('dateField') || 'DEADLINE';
    dateFieldSelect.addEventListener('change', (event) => setFilter('dateField', event.target.value));
}

function initGridActions() {
    delegateListItemActions('tasksDatatable',
        (id, elem) => editTaskService.showEditDialog(id),
        (id, name, elem) => deleteClicked(id, name),
        (id, elem) =>copyTaskService.showCopyDialog(id) ,
    )
}

export function refreshTaskGrid() {
    if (grid) {
        grid.forceRender();
    }
}