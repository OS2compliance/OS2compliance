import "../../vendor/chartjs-adapter-date-fns/chartjs-adapter-date-fns.js"

const currentYear = new Date().getFullYear();

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

    const data = config.data;

    const chartPicker = document.getElementById('diagramSelector');
    const selectedOption = chartPicker.selectedOptions[0]
    const currentEntityName = selectedOption.dataset.entityName;

    // Chart configuration based on type
    const chartConfiguration = getConfigFor(config.type, !!config.dateGrouping, data, config.title, currentEntityName)

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
        queryArray.push(`x=${config.x}`);
    }
    if (config.y) {
        queryArray.push(`y=${config.y}`);
    }
    if (config.groupTimeBy) {
        queryArray.push(`groupTimeBy=${config.groupTimeBy}`);
    }
    if (config.startDate) {
        queryArray.push(`startDate=${config.startDate}`);
    }
    if (config.endDate) {
        queryArray.push(`endDate=${config.endDate}`);
    }
    if (config.incidentFieldId) {
        queryArray.push(`incidentFieldId=${config.incidentFieldId}`);
    }
    if (queryArray.length > 0) {
        url += ('?')
        url += queryArray.join('&')
    }

    return url;
}

function getConfigFor(chartType, groupedByDate, data, title, currentEntityName) {
    const configs = {
        BAR: {
            DEFAULT: {
                type: CHARTTYPE.BAR.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: false
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                    scales: {
                        y: {
                            beginAtZero: true,
                            stepSize: 1,
                        }
                    }
                }
            },
            TIME: {
                type: CHARTTYPE.BAR.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: false
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                    scales: {
                        x: {
                            type: 'time',
                            time: {
                                unit: 'month',
                                round: 'month',
                                displayFormats: {
                                    month: 'MMM'
                                },
                                minUnit: 'month'
                            },
                            ticks: {
                                autoSkip: false,
                                stepSize: 1,
                                maxTicksLimit: 12
                            },
                            min: new Date(currentYear, 0, 1),
                            max: new Date(currentYear, 11, 31),
                        },
                        y: {
                            beginAtZero: true,
                            stepSize: 1,
                        }
                    }
                }
            }
        },
        PIE: {
            DEFAULT: {
                type: CHARTTYPE.PIE.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: true
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                }
            },
            TIME: {
                type: CHARTTYPE.PIE.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: true
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                    scales: {
                        r: {
                            type: 'time',
                            time: {
                                unit: 'month',
                                round: 'month',
                                displayFormats: {
                                    month: 'MMM'
                                },
                                minUnit: 'month'
                            },
                            min: new Date(currentYear, 0, 1),
                            max: new Date(currentYear, 11, 31),
                        },
                    }
                }
            }
        },
        STACKED_BAR: {
            DEFAULT: {
                type: CHARTTYPE.BAR.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: true
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                    scales: {
                        x: {
                            stacked: true,
                        },
                        y: {
                            stacked: true,
                            beginAtZero: true,
                            stepSize: 1,
                        }
                    }
                }
            },
            TIME: {
                type: CHARTTYPE.BAR.toLocaleLowerCase(),
                data: data,
                options: {
                    locale: 'da-DK',
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: title
                        },
                        legend: {
                            display: true
                        }
                    },
                    parsing: {
                        xAxisKey: 'x',
                        yAxisKey: 'y',
                        key: "y",
                    },
                    onClick: (e) => onChartClick(e, currentEntityName),
                    scales: {
                        x: {
                            type: 'time',
                            stacked: true,
                            time: {
                                unit: 'month',
                                round: 'month',
                                displayFormats: {
                                    month: 'MMM'
                                },
                                minUnit: 'month'
                            },
                            ticks: {
                                autoSkip: false,
                                stepSize: 1,
                                maxTicksLimit: 12
                            },
                            min: new Date(currentYear, 0, 1),
                            max: new Date(currentYear, 11, 31),
                        },
                        y: {
                            stacked: true,
                            beginAtZero: true,
                            stepSize: 1,
                        }
                    }
                }
            }
        }
    }


    switch (chartType) {
        case CHARTTYPE.BAR:
            if (groupedByDate) {
                return configs.BAR.TIME
            } else {
                return configs.BAR.DEFAULT
            }
        case CHARTTYPE.STACKED_BAR:
            if (groupedByDate) {
                return configs.STACKED_BAR.TIME
            } else {
                return configs.STACKED_BAR.DEFAULT
            }
        case CHARTTYPE.PIE:
            if (groupedByDate) {
                return configs.PIE.TIME
            } else {
                return configs.PIE.DEFAULT
            }
        default:
            return null;

    }
}