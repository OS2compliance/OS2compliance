import {fetchStatistic, renderChart, buildUrl, CHARTTYPE} from "./statistic-service.js";

export async function renderDashboardChart () {

    const config = {
        entity : 'task',
        type : CHARTTYPE.STACKED_BAR,
        x : 'nextDeadline',
        y : 'taskType',
        stack :'taskType',
        aggregation : 'count',
        groupTimeBy : 'MONTH',
        dateField : null,
        startDate : null,
        endDate : null,
        ownerOnly : true,
    }

    let url = buildUrl(config)

    const data = await fetchStatistic(url);

    renderChart(data, config.type, "Fordeling af opgaver og kontroller");
}