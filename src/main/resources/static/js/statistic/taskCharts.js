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
        startDate : '01/09-2025',
        endDate : '01/10-2025',
        ownerOnly : true,
    }

    let url = buildUrl(config)

    const data = await fetchStatistic(url);

    console.log(data);

    renderChart(data, config.type, "Fordeling af opgaver og kontroller");
}