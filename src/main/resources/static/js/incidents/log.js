import IncidentGridService from "./incident-grid-service.js";
import {initStatisticView} from "../statistic/statisticView.js";
import {IncidentService} from "./incident-service.js";

document.addEventListener("DOMContentLoaded", function(event) {
    window.incidentGridService = new IncidentGridService();
    window.incidentService = new IncidentService();
    incidentService.init();
    incidentGridService.init();

    initStatisticView('incident')
});