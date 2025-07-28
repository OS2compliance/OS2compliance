import RegisterPurposeService from "./purposeView.js";
import RegisterGeneralService from "./generalView.js";
import RegisterDataprocessingService from "./dataprocessingView.js";
import RegisterAssessmentService from "./assessmentView.js";


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
});








