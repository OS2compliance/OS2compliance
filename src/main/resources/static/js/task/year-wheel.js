import { renderYearWheel, initDetailPanelClose } from "./year-wheel-renderer.js";
import { updateFilterBar } from "./year-wheel-filter-bar.js";

var FILTER_SELECTOR_IDS = ['assignmentTypeSelector', 'taskRepetitionSelector'];
var VIEW_STORAGE_KEY = 'os2c-task-view-preference';

/**
 * Initializes the year wheel view with toggle, navigation and data fetching.
 * @param {string} yearWheelUrl - The REST endpoint URL for year wheel data
 * @param {string} token - CSRF token
 */
export function initYearWheel(yearWheelUrl, token) {
    var now = new Date();
    var state = {
        currentYear: now.getFullYear(),
        currentMonth: now.getMonth() + 1,
        selectedChip: null,
        yearWheelUrl: yearWheelUrl,
        token: token,
        activeTagFilters: new Set(),
        lastData: null
    };

    initViewToggle(state);
    initYearNavigation(state);
    initDetailPanelClose();
    initFilterListeners(state);
    initOnlyMineToggle(state);
}

/**
 * Initializes the year wheel in standalone mode (no toggle, always visible).
 * @param {string} yearWheelUrl - The REST endpoint URL
 * @param {string} token - CSRF token
 */
export function initYearWheelStandalone(yearWheelUrl, token) {
    var now = new Date();
    var state = {
        currentYear: now.getFullYear(),
        currentMonth: now.getMonth() + 1,
        selectedChip: null,
        yearWheelUrl: yearWheelUrl,
        token: token,
        activeTagFilters: new Set(),
        lastData: null
    };

    var yearWheelContent = document.getElementById('yearWheelContent');
    if (yearWheelContent) {
        yearWheelContent.style.display = '';
    }

    initYearNavigation(state);
    initDetailPanelClose();
    initOnlyMineToggle(state);
    fetchAndRender(state);
}

// --- View Toggle ---

function initViewToggle(state) {
    var btnTable = document.getElementById('btnTableView');
    var btnYearWheel = document.getElementById('btnYearWheel');
    var yearWheelContent = document.getElementById('yearWheelContent');
    var tableViewContent = document.getElementById('tasksDatatable');
    var yearNav = document.getElementById('yearNav');
    var tableOptions = document.querySelector('.tableOptionsContainer');
    var onlyMineContainer = document.getElementById('onlyMineToggleContainer');

    if (!btnTable || !btnYearWheel) {
        return;
    }

    function showTableView() {
        btnTable.classList.add('active');
        btnYearWheel.classList.remove('active');
        yearWheelContent.style.display = 'none';
        tableViewContent.style.display = '';
        yearNav.style.display = 'none';
        if (tableOptions) {
            tableOptions.style.display = '';
        }
        if (onlyMineContainer) {
            onlyMineContainer.classList.add('d-none');
        }
    }

    function showYearWheelView() {
        btnYearWheel.classList.add('active');
        btnTable.classList.remove('active');
        yearWheelContent.style.display = '';
        tableViewContent.style.display = 'none';
        yearNav.style.display = 'flex';
        if (tableOptions) {
            tableOptions.style.display = 'none';
        }
        if (onlyMineContainer) {
            onlyMineContainer.classList.remove('d-none');
        }
        fetchAndRender(state);
    }

    btnTable.addEventListener('click', function () {
        showTableView();
        try { localStorage.setItem(VIEW_STORAGE_KEY, 'table'); } catch (e) { /* ignore */ }
    });

    btnYearWheel.addEventListener('click', function () {
        showYearWheelView();
        try { localStorage.setItem(VIEW_STORAGE_KEY, 'yearwheel'); } catch (e) { /* ignore */ }
    });

    // Restore saved preference
    try {
        var saved = localStorage.getItem(VIEW_STORAGE_KEY);
        if (saved === 'yearwheel') {
            showYearWheelView();
            return;
        }
    } catch (e) {
        // localStorage not available, stay on default
    }

    // Default: fetch for year wheel in background so it's ready
    fetchAndRender(state);
}

// --- Year Navigation ---

function initYearNavigation(state) {
    var yearLabel = document.getElementById('yearLabel');
    var yearPrev = document.getElementById('yearPrev');
    var yearNext = document.getElementById('yearNext');

    if (!yearPrev || !yearNext || !yearLabel) {
        return;
    }

    yearLabel.textContent = state.currentYear;

    yearPrev.addEventListener('click', function () {
        state.currentYear--;
        yearLabel.textContent = state.currentYear;
        fetchAndRender(state);
    });

    yearNext.addEventListener('click', function () {
        state.currentYear++;
        yearLabel.textContent = state.currentYear;
        fetchAndRender(state);
    });
}

// --- Filter Listeners ---

function initFilterListeners(state) {
    FILTER_SELECTOR_IDS.forEach(function (selectorId) {
        var el = document.getElementById(selectorId);
        if (el) {
            el.addEventListener('change', function () {
                var yearWheelContent = document.getElementById('yearWheelContent');
                if (yearWheelContent && yearWheelContent.style.display !== 'none') {
                    fetchAndRender(state);
                }
            });
        }
    });
}

function initOnlyMineToggle(state) {
    var toggle = document.getElementById('onlyMineToggle');
    if (toggle) {
        toggle.addEventListener('change', function () {
            fetchAndRender(state);
        });
    }
}

// --- Data Fetching ---

function collectFilters() {
    var filters = {};

    var typeSelector = document.getElementById('assignmentTypeSelector');
    if (typeSelector && typeSelector.value && typeSelector.value !== 'null') {
        filters['taskType'] = typeSelector.value;
    }

    var repetitionSelector = document.getElementById('taskRepetitionSelector');
    if (repetitionSelector && repetitionSelector.value && repetitionSelector.value !== 'null') {
        filters['taskRepetition'] = repetitionSelector.value;
    }

    // Include active Grid.js column search filters
    var searchInputs = document.querySelectorAll('[data-search-key]');
    searchInputs.forEach(function (input) {
        if (input.value && input.value.trim() !== '') {
            filters[input.dataset.searchKey] = input.value.trim();
        }
    });

    // Admin: only my tasks toggle
    var onlyMineToggle = document.getElementById('onlyMineToggle');
    if (onlyMineToggle && onlyMineToggle.checked) {
        filters['onlyMine'] = 'true';
    }

    return filters;
}

function fetchAndRender(state) {
    var filters = collectFilters();
    var params = new URLSearchParams(filters);
    params.set('year', state.currentYear);

    fetch(state.yearWheelUrl + '?' + params.toString(), {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': state.token
        }
    })
    .then(function (response) {
        if (!response.ok) {
            throw new Error('Failed to fetch year wheel data');
        }
        return response.json();
    })
    .then(function (data) {
        renderYearWheel(data, state);
        updateFilterBar(filters);
    })
    .catch(function (error) {
        console.error('Year wheel fetch error:', error);
        renderError();
    });
}

// --- Error State ---

function renderError() {
    var grid = document.getElementById('yearGrid');
    if (grid) {
        grid.innerHTML = '';
        var template = document.getElementById('tmplYearWheelError');
        grid.appendChild(template.content.cloneNode(true));
    }
}