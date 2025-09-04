
export const CHARTTYPE = {
    BAR : "BAR",
    PIE : "PIE",
    STACKED_BAR: "STACKEDBAR",
}

export const AGGREGATION_TYPE = {
    COUNT : 'COUNT',
    SUM:'SUM',
    AVG : 'AVERAGE',
    MIN : 'MIN',
    MAX : 'MAX',
}

const defaultConfig = {
    entity : 'task',
    type : CHARTTYPE.BAR,
    x : 'name',
    y : 'id',
    aggregation : AGGREGATION_TYPE.COUNT,
    groupTimeBy : 'MONTH',
    dateField : null,
    startDate : null,
    endDate : null,
    ownerOnly : true,
}



export async function fetchStatistic(config) {
    const url = buildUrl(config)

    const response = await fetch(url)
    if (!response.ok) {
        console.error(response.error);
    }
    return await response.json();

}

export async function renderChart(config, elementId, title) {
    const ctx = document.getElementById(elementId);

    if (!ctx) {
        console.error(`could not find canvas element with id ${elementId}. Will not render chart.`);
        return;
    }

    const data = await fetchStatistic(config);

    // Chart configuration based on type
    const chartConfiguration = {
        type: config.type === CHARTTYPE.STACKED_BAR ? CHARTTYPE.BAR.toLocaleLowerCase() : config.type.toLocaleLowerCase(),
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
                    display: config.type !== CHARTTYPE.BAR
                }
            }
        }
    };

    // Add stacked configuration for stacked bar charts
    if (config.type === CHARTTYPE.STACKED_BAR) {
        chartConfiguration.options.scales = {
            x: {stacked: true},
            y: {stacked: true}
        };
    }

    return new Chart(ctx, chartConfiguration);
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