let chartInstance = null;

const CHARTTYPE = {
    BAR : "BAR",
    PIE : "PIE",
    STACKED_BAR: "STACKEDBAR",
}

export default async function testChart() {

    const entity = 'task'
    const type = CHARTTYPE.BAR
    const x = 'taskType'
    const y = 'name'
    const stack =null;
    const aggregation = 'count';
    const dateField = null;
    const startDate = Date.parse('2025-09-01')
    const endDate = Date.parse('2025-09-08')
    let url = `/rest/statistic/${entity}?type=${type.toLocaleUpperCase()}&x=${x}&y=${y}&aggregation=${aggregation}`;

    if (stack && type === 'stackedbar') {
        url += `&stack=${stack}`;
    }

    if (dateField) {
        url += `&dateField=${dateField}`;
        if (startDate) url += `&startDate=${startDate}`;
        if (endDate) url += `&endDate=${endDate}`;
    }

    const data = await fetchStatistic(url);

    renderChart(data, type.toLocaleLowerCase());

}

async function fetchStatistic(url) {
    const response = await fetch(url)
    if (!response.ok) {
        console.error(response.error);
    }
    return await response.json();

}

function renderChart(data, chartType) {
    const ctx = document.getElementById('testChart');

    // Destroy existing chart
    if (chartInstance) {
        chartInstance.destroy();
    }

    // Chart configuration based on type
    const config = {
        type: chartType === 'stackedbar' ? 'bar' : chartType,
        data: data,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: getChartTitle()
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

function getChartTitle() {
    return 'testTitle'
}