import {initStatisticView} from "../statistic/statisticView.js";
import IncidentService from "./incident-service.js";
import IncidentGridService from "./incident-grid-service.js";

document.addEventListener("DOMContentLoaded", function(event) {
    const params = new URLSearchParams(window.location.search);
    const urlFrom = params.get('from');
    if (urlFrom) {
        const fromDate = new Date(urlFrom);
        if (!isNaN(fromDate)) {
            localStorage.setItem('incidentFilterFrom', fromDate.toISOString());
        }
    }
    const urlTo = params.get('to');
    if (urlTo) {
        const toDate = new Date(urlTo);
        if (!isNaN(toDate)) {
            localStorage.setItem('incidentFilterTo', toDate.toISOString());
        }
    }

    let incidentService = new IncidentService();
    incidentService.init();

    let incidentGridService = new IncidentGridService();
    incidentGridService.init();

    initStatisticView('incident')

    initPrintReportBtn(incidentGridService)
    initGenerateExcelBtn(incidentGridService)
});

function initPrintReportBtn(incidentGridService) {
    const printReportBtn = document.getElementById('printReportBtn');
    printReportBtn.addEventListener('click', ()=> incidentGridService.generateReport())
}

function initGenerateExcelBtn(incidentGridService) {
    const generateExelBtn = document.getElementById('fetchExcelBtn');
    generateExelBtn.addEventListener('click',()=> incidentGridService.generateExcel())
}