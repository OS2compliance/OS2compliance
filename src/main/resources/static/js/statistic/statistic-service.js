let chartInstance = null;

export const CHARTTYPE = {
    BAR : "BAR",
    PIE : "PIE",
    STACKED_BAR: "STACKEDBAR",
}

const defaultConfig = {
    entity : 'task',
    type : CHARTTYPE.BAR,
    x : 'name',
    y : 'id',
    stack :null,
    aggregation : 'count',
    groupTimeBy : 'MONTH',
    dateField : null,
    startDate : null,
    endDate : null,
    ownerOnly : true,
}



export async function fetchStatistic(url) {
    const response = await fetch(url)
    if (!response.ok) {
        console.error(response.error);
    }
    return await response.json();

}

export function renderChart(data, chartType, title) {
    const ctx = document.getElementById('testChart');

    // Destroy existing chart
    if (chartInstance) {
        chartInstance.destroy();
    }

    const type =chartType.toLocaleLowerCase()

    // Chart configuration based on type
    const config = {
        type: type === 'stackedbar' ? 'bar' : type,
        data: data,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: title
                },
                legend: {
                    display: chartType !== 'pie'
                }
            }
        }
    };

    // Add stacked configuration for stacked bar charts
    if (chartType === 'stackedbar') {
        config.options.scales = {
            x: {stacked: true},
            y: {stacked: true}
        };
    }

    // Special configuration for pie charts
    if (chartType === 'pie') {
        config.options.scales = undefined;
    }

    chartInstance = new Chart(ctx, config);
}

export function buildUrl(config = defaultConfig) {
    let url = `/rest/statistic/${config.entity}?type=${config.type.toLocaleUpperCase()}&x=${config.x}&y=${config.y}&aggregation=${config.aggregation}`;

    if (config.ownerOnly) {
        url += '&ownerOnly=true';
    }

    if (config.stack && config.type === 'stackedbar') {
        url += `&stack=${config.stack}`;
    }

    if (config.groupTimeBy) {
        url += `&groupTimeBy=${config.groupTimeBy}`;
    }

    if (config.dateField) {
        url += `&dateField=${config.dateField}`;
        if (config.startDate) url += `&startDate=${config.startDate}`;
        if (config.endDate) url += `&endDate=${config.endDate}`;
    }

    return url;
}