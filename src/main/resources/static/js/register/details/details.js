import RegisterPurposeService from "./purposeView.js";
import RegisterGeneralService from "./generalView.js";
import RegisterDataprocessingService from "./dataprocessingView.js";
import RegisterAssessmentService from "./assessmentView.js";
import KLEView from "./kleView.js";
import {CreateThreatAssessmentService} from "../../risk/createThreatAssessmentService.js";


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
});


function initCreateThreatAssessmentModal () {
    const createThreatAssessmentService = new CreateThreatAssessmentService();
    createThreatAssessmentService.init();
}





