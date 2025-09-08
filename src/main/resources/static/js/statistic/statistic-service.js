
export const CHARTTYPE = {
    BAR: "BAR",
    PIE: "PIE",
    STACKED_BAR: "STACKEDBAR",
}

export const AGGREGATION_TYPE = {
    COUNT: 'COUNT',
    SUM: 'SUM',
    AVG: 'AVERAGE',
    MIN: 'MIN',
    MAX: 'MAX',
}

const defaultConfig = {
    entity: 'task',
    type: CHARTTYPE.BAR,
    x: 'name',
    y: 'id',
    aggregation: AGGREGATION_TYPE.COUNT,
    groupTimeBy: 'MONTH',
    dateField: null,
    startDate: null,
    endDate: null,
    ownerOnly: true,
}

export async function fetchStatistic(config) {
    const url = buildUrl(config)

    const response = await fetch(url)
    if (!response.ok) {
        console.error(response.error);
    }
    return await response.json();

}

export async function renderChart(argumentConfig, elementId) {
    const ctx = document.getElementById(elementId);

    if (!ctx) {
        console.error(`could not find canvas element with id ${elementId}. Will not render chart.`);
        return;
    }

    // destroy any existing instance of a chart
    const existingChart = Chart.getChart(ctx);
    if (existingChart) {
        existingChart.destroy();
    }

    const config = await fetchStatistic(argumentConfig);
    const data = config.data;

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
                    text: config.title
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
    let url = `/rest/statistic/${config.chartId}`;

    const queryArray = []
    if (config.x) {
        queryArray.push(`&x=${config.x}`);
    }
    if (config.y) {
        queryArray.push(`&y=${config.y}`);
    }
    if (config.groupTimeBy) {
        queryArray.push(`&groupTimeBy=${config.groupTimeBy}`);
    }
    if (config.startDate) {
        queryArray.push(`&startDate=${config.startDate}`);
    }
    if (config.endDate) {
        queryArray.push(`&endDate=${config.endDate}`);
    }

    if (queryArray.length > 0) {
        url += '?'
        for (const query of queryArray) {
            url += query;
        }
    }

    return url;
}