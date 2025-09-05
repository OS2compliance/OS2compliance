export async function initStatisticView(domain) {

    const statisticButton = document.getElementById('toggleStatisticButton');
    statisticButton.addEventListener('click', async (e) => openStatisticModal(domain))

}

async function openStatisticModal(domain) {
    const statisticModalContainer = document.getElementById('statisticModalContainer');
    if (!statisticModalContainer) {
        console.error('No container found for statistic container');
    }

    // Fetch modal content for this domain
    const url = `/statistic/${domain}`
    const networkService = new NetworkService();
    if (await networkService.GetFragment(url, statisticModalContainer)) {
        const modal = new bootstrap.Modal(statisticModalContainer, {
            backdrop: "static"
        })
        modal.show()
    } else {
        console.error('could not show modal for statistics')
    }
}