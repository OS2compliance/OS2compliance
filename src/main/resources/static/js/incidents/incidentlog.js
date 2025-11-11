import {initStatisticView} from "../statistic/statisticView.js";
import { incidentGridService, incidentService } from "./incident-service.js";

document.addEventListener("DOMContentLoaded", function(event) {
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