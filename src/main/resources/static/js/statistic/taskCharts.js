import {renderChart, CHARTTYPE, AGGREGATION_TYPE} from "./statistic-service.js";

export async function renderDashboardChart () {

    const config = {
        entity : 'task',
        type : CHARTTYPE.STACKED_BAR,
        x : 'nextDeadline',
        y : 'taskType',
        aggregation : AGGREGATION_TYPE.COUNT,
        groupTimeBy : 'MONTH',
        dateField : 'nextDeadline',
        startDate : '01/01-2025',
        endDate : '24/12-2025',
        ownerOnly : true,
    }

    await renderChart(config, 'testChart', "Fordeling af opgaver og kontroller");
}

export async function renderOverdueChart () {
    const now = uiFormatDate(new Date());

    const config = {
        entity : 'task',
        type : CHARTTYPE.BAR,
        x : 'responsibleOu.name',
        y : 'responsibleOu',
        aggregation : AGGREGATION_TYPE.COUNT,
        groupTimeBy : null,
        dateField : 'nextDeadline',
        startDate : null,
        endDate : now,
        ownerOnly : false,
    }

    await renderChart(config, 'testChart2', "Overskredne opgaver");
}

export async function renderTaskStatusChart () {

    const config = {
        entity : 'task',
        type : CHARTTYPE.PIE,
        x : 'status',
        y : 'status',
        aggregation : AGGREGATION_TYPE.COUNT,
        groupTimeBy : null,
        dateField : 'null',
        startDate : null,
        endDate : null,
        ownerOnly : false,
    }

    await renderChart(config, 'testChart3', "Øjebliksbillede af opgaver");
}

function uiFormatDate(date) {
    if (date === null || date === '') {
        return '';
    }
    let dd = "" + date.getDate();
    if (dd.length === 1) {
        dd = "0" + dd;
    }
    let mm = "" + (date.getMonth()+1);
    if (mm.length === 1) {
        mm = "0" + mm;
    }
    let yyyy = date.getFullYear();
    return `${dd}/${mm}-${yyyy}`;
}