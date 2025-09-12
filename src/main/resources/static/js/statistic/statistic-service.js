// import "../../vendor/date-fns/date-fns.js"
import "../../vendor/chartjs-adapter-date-fns/chartjs-adapter-date-fns.js"


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

export function destroyChart(chartCanvas) {
    const existingChart = Chart.getChart(chartCanvas);
    if (existingChart) {
        existingChart.destroy();
    }
}

export async function renderChart(argumentConfig, elementId) {
    const ctx = document.getElementById(elementId);

    if (!ctx) {
        console.error(`could not find canvas element with id ${elementId}. Will not render chart.`);
        return;
    }

    // destroy any existing instance of a chart
    destroyChart(ctx)

    const config = await fetchStatistic(argumentConfig);

    console.log(config)
    const data = config.data;

    const chartPicker = document.getElementById('diagramSelector');
    const selectedOption = chartPicker.selectedOptions[0]
    const currentEntityName = selectedOption.dataset.entityName;

    console.log(config)

    // Chart configuration based on type
    const chartConfiguration = {
        type: config.type === CHARTTYPE.STACKED_BAR ? CHARTTYPE.BAR.toLocaleLowerCase() : config.type.toLocaleLowerCase(),
        data: data,
        options: {
            locale: 'da-DK',
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
            },
            parsing: {
                xAxisKey: 'x',
                yAxisKey: 'y',
                key: "y",
            },
            onClick: (e) => onChartClick(e, currentEntityName),
            scales: {
                x: {},
                y: {}
            }
        }
    };

    console.log(config.xscaleDateType)
    if (config.xscaleDateType) {
        chartConfiguration.options.scales.x.type = 'time'
        chartConfiguration.options.scales.x.time = {
            unit: 'month'
        }
    }
    console.log(chartConfiguration)

    // Add stacked configuration for stacked bar charts
    if (config.type === CHARTTYPE.STACKED_BAR) {
        chartConfiguration.options.scales.x.stacked = true
        chartConfiguration.options.scales.y.stacked = true
    }

    return new Chart(ctx, chartConfiguration);
}

async function onChartClick(e, entityName) {
    const chart = e.chart
    const elements = chart.getElementsAtEventForMode(e, 'nearest', {intersect: true}, true);
    if (elements.length > 0) {
        const element = elements[0];
        const dataPoint = chart.data.datasets[element.datasetIndex].data[element.index];
        const entityIds = dataPoint.entityIds

        let url = `/statistic/chart/${entityName}/entityList`
        if (entityIds) {
            url += `?entityIds=${entityIds.join(',')}`
        }

        const entityListcontainer = document.getElementById('relevantEntityListContainer');
        const networkService = new NetworkService();
        if (await networkService.GetFragment(url, entityListcontainer)) {
            // Init here if needed
        }
    }
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
    if (config.incidentFieldId) {
        queryArray.push(`&incidentFieldId=${config.incidentFieldId}`);
    }

    if (queryArray.length > 0) {
        url += '?'
        for (const query of queryArray) {
            url += query;
        }
    }

    return url;
}