

const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

document.addEventListener("DOMContentLoaded", function (event) {

    initSystemOwnerRapportButton()

    const showDashboard = document.getElementById('tasksDatatable');
    if (showDashboard) {
        const today = new Date();
        let gridConfigTasks = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Opgavenavn",
                    searchable: {searchKey: 'name'},
                    formatter: (cell, row) => {
                        const url = tasksViewUrl + row.cells[0]['data'] + "?referral=dashboard";
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    }
                },
                {
                    name: "Opgave type",
                    searchable: {
                        searchKey: 'taskType',
                        fieldId: "assignmentTypeSelector"
                    }
                },
                {
                    name: "Ansvarlig",
                    searchable: {searchKey: 'responsibleUser.name'},
                    hidden: true
                },
                {
                    name: "Afdeling",
                    searchable: {searchKey: 'responsibleOU.name'}
                },
                {
                    name: "Deadline",
                    searchable: {searchKey: 'nextDeadline'},
                    formatter: (cell, row) => {
                        var completed = row.cells[7]['data'];
                        var type = row.cells[2]['data'];
                        if (completed && type == "Opgave") {
                            return gridjs.html(`<span>${cell}</span>`);
                        }
                        var dateString = cell.replace(" ", "/");
                        dateString = dateString.replace("-", "/");
                        var dateSplit = dateString.split("/");
                        var cellDate = new Date(dateSplit[2] + "-" + dateSplit[1] + "-" + dateSplit[0] + "T23:59:59");
                        if (cellDate < today) {
                            return gridjs.html(`<span style="color: red;">${cell}</span>`);
                        } else {
                            return gridjs.html(`<span>${cell}</span>`);
                        }
                    }
                },
                {
                    name: "Gentages",
                    searchable: {searchKey: 'taskRepetition', fieldId: 'taskRepetitionSelector'},
                },
                {
                    name: "Status",
                    searchable: {
                        sortKey: 'completed'
                    },
                    formatter: (cell, row) => {
                        var status = "";

                        // Null-safe access to row cells and data
                        var type = row?.cells?.[2]?.data || null;
                        var deadline = row?.cells?.[5]?.data || null;

                        // if completed and task type opgave
                        if (cell && type === "Opgave") {
                            status = '<div class="d-block badge bg-success">Udført</div>';
                        } else if (deadline) {
                            // Only process deadline if it exists
                            var dateString = deadline.replace(" ", "/");
                            dateString = dateString.replace("-", "/");
                            var dateSplit = dateString.split("/");

                            // Validate that we have enough date parts
                            if (dateSplit.length >= 3) {
                                var deadlineAsDate = new Date(dateSplit[2] + "-" + dateSplit[1] + "-" + dateSplit[0] + "T23:59:59");

                                // Make sure today is defined (you might need to define this elsewhere if not already)
                                var today = new Date();

                                if (deadlineAsDate < today) {
                                    status = '<div class="d-block badge bg-danger">Overskredet</div>';
                                } else {
                                    status = '<div class="d-block badge bg-warning">Ikke udført</div>';
                                }
                            } else {
                                // Invalid date format fallback
                                status = '<div class="d-block badge bg-secondary">Ugyldig dato</div>';
                            }
                        } else {
                            // No deadline available
                            status = '<div class="d-block badge bg-secondary">Ingen deadline</div>';
                        }

                        return gridjs.html(status, 'div');
                    },
                },
                {
                    name: "Tags",
                    searchable: {searchKey: 'tags'},
                    formatter: (cell, row) => {
                        var result = '<ul>';
                        if (cell != null && cell.trim() !== '') {
                            var tags = cell.split(',');
                            for (var i = 0; i < tags.length; i++) {
                                result += '<li>' + tags[i] + '</li>';
                            }
                        }

                        result += '</ul>';
                        return gridjs.html(''.concat(...result), 'div')
                    },
                }
            ],
            server: {
                url: gridTasksUrl + "/" + userId,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(task =>
                    [task.id, task.name, task.taskType, task.responsibleUser, task.responsibleOU, task.nextDeadline, task.taskRepetition, task.completed, task.tags]
                ),
                total: data => data.totalCount ? data.totalCount : 0
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

        const gridTasks = new gridjs.Grid(gridConfigTasks)
            .render(document.getElementById("tasksDatatable"));

        //Enables custom column search, serverside sorting and pagination
        new CustomGridFunctions(gridTasks, gridTasksUrl + "/" + userId, 'tasksDatatable')


        let gridConfigAssets = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Navn",
                    searchable: {searchKey: 'name'},
                    formatter: (cell, row) => {
                        const url = assetsViewUrl + row.cells[0]['data'];
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    }
                },
                {
                    name: "Leverandør",
                    searchable: {searchKey: 'supplier'},
                },
                {
                    name: "Type",
                    searchable: {searchKey: 'assetType'},
                },
                {
                    name: "Systemejer",
                    hidden: true
                },
                {
                    name: "Opdateret",
                    searchable: {searchKey: 'updatedAt'},
                },
                {
                    name: "Risikovurdering",
                    searchable: {searchKey: 'assessment'},
                },
                {
                    name: "Status",
                    searchable: {searchKey: 'assetStatus', fieldId: 'assetStatusSearchSelector'},
                    formatter: (cell, row) => {
                        var status = cell;
                        if (cell === "Ikke startet") {
                            status = [
                                '<div class="d-block badge bg-warning">' + cell + '</div>'
                            ]
                        } else if (cell === "I gang") {
                            status = [
                                '<div class="d-block badge bg-info">' + cell + '</div>'
                            ]
                        } else if (cell === "Klar") {
                            status = [
                                '<div class="d-block badge bg-success">' + cell + '</div>'
                            ]
                        }
                        return gridjs.html(''.concat(...status), 'div')
                    },
                }
            ],
            server: {
                url: gridAssetsUrl + "/" + userId,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(asset =>
                    [asset.id, asset.name, asset.supplier, asset.assetType, asset.responsibleUser, asset.updatedAt, asset.criticality, asset.assetStatus]
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

        const gridAssets = new gridjs.Grid(gridConfigAssets).render(document.getElementById("assetsDatatable"));

        //Enables custom column search, serverside sorting and pagination
        new CustomGridFunctions(gridAssets, gridAssetsUrl + "/" + userId, 'assetsDatatable')


        let gridConfigRegisters = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Titel",
                    searchable: {
                        searchKey: 'name'
                    },
                    formatter: (cell, row) => {
                        const url = registersViewUrl + row.cells[0]['data'];
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    },
                    width: '40%'
                },
                {
                    name: "Ansvarlig afdeling",
                    searchable: {
                        searchKey: 'responsibleOUNames'
                    },
                },
                {
                    name: "Kontakt person",
                    hidden: true
                },
                {
                    name: "Senest redigeret",
                    searchable: {
                        searchKey: 'updatedAt'
                    },
                },
                {
                    name: "Konsekvens vurdering",
                    width: "150px",
                    searchable: {
                        searchKey: 'consequence',
                        fieldId: 'registerConsequenceSearchSelector'
                    },
                    formatter: (cell, row) => {
                        var assessment = cell;
                        if (cell === "Grøn") {
                            assessment = [
                                '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Gul") {
                            assessment = [
                                '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Rød") {
                            assessment = [
                                '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>'
                            ]
                        }
                        return gridjs.html(''.concat(...assessment), 'div')
                    }
                },
                {
                    name: "Status",
                    width: "150px",
                    searchable: {
                        searchKey: 'status',
                        fieldId: 'registerStatusSearchSelector'
                    },
                    formatter: (cell, row) => {
                        var status = cell;
                        if (cell === "Klar") {
                            status = [
                                '<div class="d-block badge bg-success" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "I gang") {
                            status = [
                                '<div class="d-block badge bg-info" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Ikke started") {
                            status = [
                                '<div class="d-block badge bg-danger" style="width: 60px">' + cell + '</div>'
                            ]
                        }
                        return gridjs.html(''.concat(...status), 'div')
                    },
                }
            ],
            server: {
                url: gridRegistersUrl + "/" + userId,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(register =>
                    [register.id, register.name, register.responsibleOU, register.responsibleUser, register.updatedAt, register.consequence, register.status]
                ),
                total: data => data.count
            },
            language: {
                'search': {
                    'placeholder': 'Søg'
                },
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': 'Fortegnelser',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };

        const gridRegisters = new gridjs.Grid(gridConfigRegisters).render(document.getElementById("registersDatatable"));

        //Enables custom column search, serverside sorting and pagination
        new CustomGridFunctions(gridRegisters, gridRegistersUrl + "/" + userId, 'registersDatatable')

        let gridConfigDocuments = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name: "Titel",
                    searchable: {
                        searchKey: 'name'
                    },
                    formatter: (cell, row) => {
                        const url = documentsViewUrl + row.cells[0]['data'];
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    }
                },
                {
                    name: "Dokumentype",
                    searchable: {
                        searchKey: 'documentType',
                        fieldId: 'documentTypeStatusSearchSelector'
                    },
                    width: '150px'
                },
                {
                    name: "Ansvarlig",
                    hidden: true
                },
                {
                    name: "Næste revidering",
                    searchable: {
                        searchKey: 'nextRevision'
                    }
                },
                {
                    name: "Status",
                    searchable: {
                        searchKey: 'status',
                        fieldId: 'documentStatusSearchSelector'
                    },
                    formatter: (cell, row) => {
                        var status = cell;
                        if (cell === "Ikke startet") {
                            status = [
                                '<div class="d-block badge bg-warning">' + cell + '</div>'
                            ]
                        } else if (cell === "I gang") {
                            status = [
                                '<div class="d-block badge bg-info">' + cell + '</div>'
                            ]
                        } else if (cell === "Klar") {
                            status = [
                                '<div class="d-block badge bg-success">' + cell + '</div>'
                            ]
                        }

                        return gridjs.html(''.concat(...status), 'div')
                    },
                },
                {
                    name: "Tags",
                    searchable: {
                        searchKey: 'tags'
                    },
                    formatter: (cell, row) => {
                        var result = '<ul>';
                        if (cell != null && cell.trim() !== '') {
                            var tags = cell.split(',');
                            for (var i = 0; i < tags.length; i++) {
                                result += '<li>' + tags[i] + '</li>';
                            }
                        }

                        result += '</ul>';
                        return gridjs.html(''.concat(...result), 'div')
                    },
                }
            ],
            server: {
                url: gridDocumentsUrl + "/" + userId,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(document =>
                    [document.id, document.name, document.documentType, document.responsibleUserId, document.nextRevision, document.status, document.tags]
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
        const gridDocuments = new gridjs.Grid(gridConfigDocuments).render(document.getElementById("documentsDatatable"));

        //Enables custom column search, serverside sorting and pagination
        new CustomGridFunctions(gridDocuments, gridDocumentsUrl + "/" + userId, 'documentsDatatable')
    }
});

function initSystemOwnerRapportButton() {
    const url = "reports/overview/systemowner"
    const button = document.getElementById("systemOwnerRapportButton");
    const buttonTextElement = button?.querySelector(".btn-text")
    const originalText = buttonTextElement?.textContent
    button?.addEventListener("click", async (e) => {
        button.disabled = true;
        button.classList.add("disabled");
        buttonTextElement.textContent = "Henter...";

        await downloadFile(url)

        button.disabled = false;
        button.classList.remove("disabled");
        buttonTextElement.textContent = originalText;
    })

    /**
     * Emulates a normal browser download
     * @param url url
     * @returns {Promise<void>}
     */
    async function downloadFile(url) {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();

        // Extract filename from Content-Disposition header
        let filename = 'System Ejer Rapport'; // fallback
        const contentDisposition = response.headers.get('Content-Disposition');
        if (contentDisposition) {
            const filenameMatch = RegExp(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/).exec(contentDisposition);
            if (filenameMatch?.[1]) {
                filename = filenameMatch[1].replace(/['"]/g, '');
            }
        }

        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename;
        a.click();

        URL.revokeObjectURL(a.href);
        a.remove();
    }
}