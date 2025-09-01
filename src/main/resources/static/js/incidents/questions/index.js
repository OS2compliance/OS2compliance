import IncidentQuestionService from "../incident-question-service.js";

const formUrl = "/incidents/questionForm"
const restUrl = "/rest/incidents/questions"

let incidentGrid;
let token = document.getElementsByName("_csrf")[0].getAttribute("content");
let incidentQuestionService = new IncidentQuestionService();

document.addEventListener("DOMContentLoaded", function(event) {
    window.incidentQuestionService = new IncidentQuestionService();

    incidentQuestionService.createQuestion("createQuestionDialog");

    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    incidentGrid = new gridjs.Grid({
        className: defaultClassName,
        sort: false,
        columns: [
            {
                id: "id",
                hidden: true
            },
            {
                id: "index",
                name: "Oversigts navn"
            },
            {
                id: "question",
                name: "Spørgsmål"
            },
            {
                id: "type",
                name: "Svartype"
            },
            {
                id: "actions",
                name: "Handlinger",
                sort: 0,
                width: '130px',
                formatter: (cell, row) => {
                    const id = row.cells[0]['data'];
                    const question = row.cells[2]['data'];
                    let lastRow = incidentQuestionService.gridFindPreviousRow(incidentGrid, id);
                    let nextRow = incidentQuestionService.gridFindNextRow(incidentGrid, id);
                    const upStyle = (lastRow === null) ? 'visibility: hidden' : '';
                    const downStyle = (nextRow === null) ? 'visibility: hidden' : '';
                    const upButton = `<button style="${upStyle}" type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentQuestionService.sortQuestionUp(incidentGrid, '${id}')"><i class="pli-up fs-5"></i></button>`;
                    const downButton = `<button style="${downStyle}" type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentQuestionService.sortQuestionDown(incidentGrid, '${id}')"><i class="pli-down fs-5"></i></button>`;

                    return gridjs.html(upButton
                        + downButton
                        + `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentQuestionService.editQuestion('editQuestionDialog', '${id}')"><i class="pli-pencil fs-5"></i></button>`
                        + `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="incidentQuestionService.deleteQuestion(incidentGrid, '${id}', '${question}')"><i class="pli-trash fs-5"></i></button>`);
                }
            }
        ],
        server:{
            url: restUrl,
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            then: data => {
                incidentGrid.data = data;
                return data.map(field => [ field.id, field.indexColumnName, field.question, field.incidentType ]);
            },
            total: data => data.totalCount
        }
    });
    incidentGrid.render(document.getElementById("incidentFieldsTable"));

    initSaveAsExcelButtonWithDefaultGrid('incidentFieldsTable', 'Hændelse_Opsætning')
});