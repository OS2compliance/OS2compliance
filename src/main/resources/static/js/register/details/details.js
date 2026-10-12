import RegisterPurposeService from "./purposeView.js";
import RegisterGeneralService from "./generalView.js";
import RegisterDataprocessingService from "./dataprocessingView.js";
import RegisterAssessmentService from "./assessmentView.js";
import KLEView from "./kleView.js";
import {CreateThreatAssessmentService} from "../../risk/createThreatAssessmentService.js";
import { printRegister } from "../register-print.js";


// Global variable to support onclick functionality. Should be removed when be have moved to addEventListeners
window.registerPurposeService = new RegisterPurposeService();
window.registerGeneralService = new RegisterGeneralService();
window.registerAssessmentService = new RegisterAssessmentService();
window.registerDataprocessingService = new RegisterDataprocessingService();

document.addEventListener("DOMContentLoaded", function () {
    registerGeneralService.init();
    registerPurposeService.init();
    registerAssessmentService.init();
    registerDataprocessingService.init();

    const kleView = new KLEView();
    kleView.init()

    initCreateThreatAssessmentModal()

    initPrintButton()
});


function initCreateThreatAssessmentModal () {
    const createThreatAssessmentService = new CreateThreatAssessmentService();
    createThreatAssessmentService.init();
}

function initPrintButton() {
    const printButton = document.getElementById('printRegisterButton');
    printButton?.addEventListener('click', () => {
        const registerId = document.getElementById('breadcrump-register-name').dataset.registerId;
        printRegister(registerId, printButton);
    });
}





