import {initStatisticView} from "../statistic/statisticView.js";
import IncidentService from "./incident-service.js";
import IncidentGridService from "./incident-grid-service.js";

document.addEventListener("DOMContentLoaded", function(event) {
    applyUrlDateFilters();

    let incidentService = new IncidentService();
    incidentService.init();

    let incidentGridService = new IncidentGridService();
    incidentGridService.init();

    initStatisticView('incident')

    initPrintReportBtn(incidentGridService)
    initGenerateExcelBtn(incidentGridService)
    initClearFiltersBtn(incidentGridService)
});

/**
 * The risk assessment view links here with `?from=&to=&assetIds=` (ISO dates, comma separated ids) to
 * jump straight to the incidents behind its "incidents last 12 months" count. CustomGridFunctions never
 * looks at the query string — it restores its filters from its own localStorage blob (keyed by the list
 * endpoint, see custom-grid-functions.js `saveState`/`loadState`) — so the filters have to be written
 * into that same blob, in the shape the grid already uses, before the grid is constructed.
 * <p>
 * The link is only meant to filter that one visit: without a URL to compare against, a later plain
 * visit to `/incidents/logs` has no way to tell "a link set this on purpose" from "a link set this
 * three weeks ago and I never noticed" — so every load without these params clears them instead of
 * leaving whatever an earlier visit happened to leave behind.
 */
function applyUrlDateFilters() {
    const params = new URLSearchParams(window.location.search);
    const from = params.get('from');
    const to = params.get('to');
    const assetIds = params.get('assetIds');

    const stateKey = `${restUrl}list_search`;
    const state = JSON.parse(localStorage.getItem(stateKey)) || {
        sortDirection: 'DESC',
        sortColumn: 'createdAt',
        page: 0,
        limit: 50,
        searchValues: {}
    };
    state.page = 0;

    if (!from && !to && !assetIds) {
        delete state.searchValues.fromDate;
        delete state.searchValues.toDate;
        delete state.searchValues.assetIds;
        localStorage.setItem(stateKey, JSON.stringify(state));
        return;
    }

    state.searchValues.dateField = 'CREATED';
    if (from) {
        state.searchValues.fromDate = formatDateToDdMmYyyy(from);
    } else {
        delete state.searchValues.fromDate;
    }
    if (to) {
        state.searchValues.toDate = formatDateToDdMmYyyy(to);
    } else {
        delete state.searchValues.toDate;
    }
    if (assetIds) {
        state.searchValues.assetIds = assetIds;
    } else {
        delete state.searchValues.assetIds;
    }
    localStorage.setItem(stateKey, JSON.stringify(state));
}

function initPrintReportBtn(incidentGridService) {
    const printReportBtn = document.getElementById('printReportBtn');
    printReportBtn.addEventListener('click', ()=> incidentGridService.generateReport())
}

function initGenerateExcelBtn(incidentGridService) {
    const generateExelBtn = document.getElementById('fetchExcelBtn');
    generateExelBtn.addEventListener('click',()=> incidentGridService.generateExcel())
}

function initClearFiltersBtn(incidentGridService) {
    const clearFiltersBtn = document.getElementById('clearFiltersBtn');
    clearFiltersBtn.addEventListener('click', () => incidentGridService.clearFilters());
}