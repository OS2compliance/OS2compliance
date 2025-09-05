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
        initChartPicker()
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
    } else {
        console.error('could not load additional config options for chart ' + chartId);
    }
}

function initChartPicker() {
    const chartPicker = document.getElementById('diagramSelector');
    chartPicker?.addEventListener('change', async (e) => {
        const selectedOption =  chartPicker.selectedOptions[0]
        const value = selectedOption.value;
        const entityName = selectedOption.dataset.entityName;
        await getConfigForChart(value, entityName);
    })
}