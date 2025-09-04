import {fetchStatistic, renderChart, buildUrl, CHARTTYPE} from "./statistic-service.js";

export async function renderDashboardChart () {

    const config = {
        entity : 'task',
        type : CHARTTYPE.STACKED_BAR,
        x : 'nextDeadline',
        y : 'taskType',
        aggregation : 'count',
        groupTimeBy : 'MONTH',
        dateField : 'nextDeadline',
        startDate : '01/01-2025',
        endDate : '24/12-2025',
        ownerOnly : true,
    }

    let url = buildUrl(config)

    const data = await fetchStatistic(url);

    renderChart(data, 'testChart', config.type, "Fordeling af opgaver og kontroller");
}

export async function renderOverdueChart () {
    const now = uiFormatDate(new Date());

    const config = {
        entity : 'task',
        type : CHARTTYPE.BAR,
        x : 'responsibleUser.name',
        y : 'responsibleUser',
        aggregation : 'count',
        groupTimeBy : null,
        dateField : 'nextDeadline',
        startDate : null,
        endDate : now,
        ownerOnly : false,
    }

    let url = buildUrl(config)

    const data = await fetchStatistic(url);

    renderChart(data, 'testChart2', config.type, "Overskredne opgaver");
}

export async function renderTaskStatusChart () {

    const config = {
        entity : 'taskGrid',
        type : CHARTTYPE.PIE,
        x : 'status',
        y : 'status',
        aggregation : 'count',
        groupTimeBy : null,
        dateField : 'null',
        startDate : null,
        endDate : null,
        ownerOnly : false,
    }

    let url = buildUrl(config)

    const data = await fetchStatistic(url);

    console.log(data)

    renderChart(data, 'testChart3', config.type, "Øjebliksbillede af opgaver");
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