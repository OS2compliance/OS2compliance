
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

export function renderChart(dataConfig, elementId, chartType, title) {
    const ctx = document.getElementById(elementId);

    // Chart configuration based on type
    const config = {
        type: chartType === CHARTTYPE.STACKED_BAR ? CHARTTYPE.BAR.toLocaleLowerCase() : chartType.toLocaleLowerCase(),
        data: dataConfig,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: title
                },
                legend: {
                    display: chartType !== CHARTTYPE.BAR
                }
            }
        }
    };

    // Add stacked configuration for stacked bar charts
    if (chartType === CHARTTYPE.STACKED_BAR) {
        config.options.scales = {
            x: {stacked: true},
            y: {stacked: true}
        };
    }

    return new Chart(ctx, config);
}

export function buildUrl(config = defaultConfig) {
    const upperCaseType = config.type.toLocaleUpperCase()
    let url = `/rest/statistic/${config.entity}?type=${upperCaseType}&x=${config.x}&y=${config.y}&aggregation=${config.aggregation}`;

    if (config.ownerOnly) {
        url += '&ownerOnly=true';
    }

    if (config.stack && upperCaseType === CHARTTYPE.STACKED_BAR) {
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