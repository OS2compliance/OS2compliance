import IncidentGridService from "./incident-grid-service.js";
import {initStatisticView} from "../statistic/statisticView.js";
import IncidentService from "./incident-service.js";

document.addEventListener("DOMContentLoaded", function(event) {
    window.incidentGridService = new IncidentGridService();
    window.incidentService = new IncidentService();
    incidentService.init();
    incidentGridService.init();

    initStatisticView('incident')

    initPrintReportBtn()
    initGenerateExcelBtn()
});

function initPrintReportBtn() {
    const printReportBtn = document.getElementById('printReportBtn');
    printReportBtn.addEventListener('click', ()=> incidentGridService.generateReport())
}

function initGenerateExcelBtn() {
    const generateExelBtn = document.getElementById('fetchExcelBtn');
    generateExelBtn.addEventListener('click',()=> incidentGridService.generateExcel())
}