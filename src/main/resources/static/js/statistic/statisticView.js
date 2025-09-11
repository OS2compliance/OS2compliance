import {renderChart} from "./statistic-service.js";

export async function initStatisticView(domain) {

    const statisticButton = document.getElementById('toggleStatisticButton');
    statisticButton.addEventListener('click', async (e) => openStatisticModal(domain))

}

async function openStatisticModal(domain) {
    const statisticModalContainer = document.getElementById('statisticModalContainer');
    if (!statisticModalContainer) {
        console.error('No container found for statistic container');
        return;
    }

    // Fetch modal content for this domain
    const url = `/statistic/${domain}`
    const networkService = new NetworkService();
    if (await networkService.GetFragment(url, statisticModalContainer)) {
        const modal = new bootstrap.Modal(statisticModalContainer, {
            backdrop: "static"
        })
        await initChartPicker()
        initFooterButtons()
        modal.show()
    } else {
        console.error('could not show modal for statistics')
    }
}

async function getConfigForChart(chartId, entityName) {
    if (!chartId) {
        console.error('No chart id found for chartId');
        return;
    }
    const additionalOptionsContainer = document.getElementById('additionalOptionsContainer');
    if (!additionalOptionsContainer) {
        console.error('No additionalOptionsContainer for chartId');
        return;
    }

    const url = `/statistic/chart/${entityName}/${chartId}`
    const networkService = new NetworkService();
    if (await networkService.GetFragment(url, additionalOptionsContainer)) {
        additionalOptionsContainer.hidden = false

        initDatePicker('fromTimePicker',)
        initDatePicker('toTimePicker',)
    } else {
        console.error('could not load additional config options for chart ' + chartId);
    }
}

async function initChartPicker() {
    const chartPicker = document.getElementById('diagramSelector');
    chartPicker?.addEventListener('change', async (e) => {
        const selectedOption = chartPicker.selectedOptions[0]
        const value = selectedOption.value;
        const entityName = selectedOption.dataset.entityName;
        await getConfigForChart(value, entityName);
    })

    // Gets fields for for the first in list on load
    const selectedOption = chartPicker.selectedOptions[0]
    const value = selectedOption.value;
    const entityName = selectedOption.dataset.entityName;
    await getConfigForChart(value, entityName);
}

function initFooterButtons() {
    const generateBtn = document.getElementById('generateChartButton');
    generateBtn.addEventListener('click', async (e) => {

        const title = document.getElementById('titleElement');
        const collectedConfig = collectChartConfig();
        await renderChart(
            collectedConfig,
            'diagramCanvas',
            title?.textContent.trim() || 'Unavngivet diagram');
    })
}

function collectChartConfig() {

    const xChoice = document.getElementById('xAxisSelector')?.selectedOptions[0]
    const yChoice = document.getElementById('yAxisSelector')?.selectedOptions[0]
    const chartId = document.getElementById('diagramSelector')?.selectedOptions[0]?.value;
    const x = xChoice?.value;
    const y = yChoice?.value;
    const groupTimeBy = document.getElementById('periodGroupingSelector')?.selectedOptions[0]?.value;
    const startDate = document.getElementById('fromTimePicker')?.value
    const endDate = document.getElementById('toTimePicker')?.value
    const dateField = document.getElementById('dateField')?.selectedOptions[0]?.value;
    const incidentFieldId = yChoice?.dataset.incidentFieldId;

    const config = {
        chartId: chartId,
        x: x,
        y: y,
        groupTimeBy: groupTimeBy,
        dateField: dateField,
        startDate: startDate,
        endDate: endDate,
        incidentFieldId: incidentFieldId,
    }

    return config;
}

function initDatePicker(id) {
    return MCDatepicker.create({
        el: `#${id}`,
        autoClose: true,
        dateFormat: 'dd/mm-yyyy',
        closeOnBlur: true,
        firstWeekday: 1,
        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
        customClearBTN: "Ryd",
        customCancelBTN: "Annuller"
    });
}
