/**
 * Risk Assessment Formatter Module
 * Generic module for formatting risk assessment data for grid display
 * Can be used for both assets and registers
 */

let templatesLoaded = false;
const templates = {};

/**
 * Loads HTML templates into memory
 */
function loadTemplates() {
    if (templatesLoaded) return;

    templates.riskCell = document.getElementById('risk-assessment-cell-template');
    templates.tooltipSection = document.getElementById('tooltip-section-template');
    templates.tooltipRow = document.getElementById('tooltip-row-template');
    templates.threatTypeBadge = document.getElementById('threat-type-badge-template');
    templates.catalogBadge = document.getElementById('threat-catalog-badge-template');
    templates.emptyState = document.getElementById('risk-empty-state-template');

    templatesLoaded = true;
}

/**
 * Converts DOM element to HTML string
 * @param {HTMLElement|DocumentFragment} element - Element to convert
 * @returns {string} HTML string
 */
function elementToString(element) {
    const container = document.createElement('div');
    container.appendChild(element);
    return container.innerHTML;
}

/**
 * Formats threat types as badges
 * @param {string} threatTypeList - Comma-separated list of threat types
 * @returns {Object} GridJS HTML object
 */
export function formatThreatTypes(threatTypeList) {
    loadTemplates();

    if (!threatTypeList || threatTypeList.trim() === '') {
        return '';
    }

    const container = document.createElement('div');
    container.className = 'd-flex flex-wrap';

    const types = threatTypeList.split(', ');
    types.forEach(type => {
        const badge = templates.threatTypeBadge.content.cloneNode(true);
        const badgeElement = badge.querySelector('[data-type-text]');
        badgeElement.textContent = type;
        container.appendChild(badge);
    });

    return gridjs.html(container.innerHTML);
}

/**
 * Formats threat catalogs as badges
 * @param {string} catalogList - Comma-separated list of catalog names
 * @returns {Object} GridJS HTML object
 */
export function formatThreatCatalogs(catalogList) {
    loadTemplates();

    if (!catalogList || catalogList.trim() === '') {
        return '';
    }

    const container = document.createElement('div');
    container.className = 'd-flex flex-wrap';

    const catalogs = catalogList.split(', ');
    catalogs.forEach(catalog => {
        const badge = templates.catalogBadge.content.cloneNode(true);
        const badgeElement = badge.querySelector('[data-catalog-text]');
        badgeElement.textContent = catalog;
        container.appendChild(badge);
    });

    return gridjs.html(container.innerHTML);
}

/**
 * Gets color class based on risk value (1-4+ scale)
 * Uses the same color scheme as risk assessments
 * @param {number} value - Risk value
 * @returns {string} Bootstrap color class
 */
function getRiskColorClass(value) {
    if (value === null || value === undefined) return 'bg-gray';

    if (value == 0) return 'bg-gray';           // grey: 0
    if (value < 2) return 'bg-green';           // Green: < 2
    if (value < 3) return 'bg-yellow-500';      // Yellow: 2-2.99
    if (value < 4) return 'bg-orange';          // Orange: 3-3.99
    return 'bg-red';                            // Red: >= 4
}

/**
 * Formats risk assessment with probability and consequence
 * Shows detailed breakdown on hover
 * @param {*} cell - Cell data (unused)
 * @param {Object} row - Row data
 * @param {Object} riskData - Risk assessment data object
 * @returns {Object} GridJS HTML object
 */
export function formatRiskAssessment(cell, row, riskData) {
    loadTemplates();

    if (!riskData || (!riskData.avgProbability && !riskData.avgConsequenceOverall)) {
        const empty = templates.emptyState.content.cloneNode(true);
        return gridjs.html(elementToString(empty));
    }

    const prob = riskData.avgProbability ? riskData.avgProbability.toFixed(1) : '—';
    const cons = riskData.avgConsequenceOverall ? riskData.avgConsequenceOverall.toFixed(1) : '—';

    const probColor = getRiskColorClass(riskData.avgProbability);
    const consColor = getRiskColorClass(riskData.avgConsequenceOverall);

    const cellClone = templates.riskCell.content.cloneNode(true);

    // Set probability
    const probBadge = cellClone.querySelector('.prob-badge');
    probBadge.textContent = prob;
    probBadge.classList.add(probColor);

    // Set consequence
    const consBadge = cellClone.querySelector('.cons-badge');
    consBadge.textContent = cons;
    consBadge.classList.add(consColor);

    // Build tooltip
    const tooltipContainer = cellClone.querySelector('[data-tooltip-content]');
    buildTooltip(tooltipContainer, riskData);

    return gridjs.html(elementToString(cellClone));
}

/**
 * Builds the detailed tooltip content
 * Grouped by level (Registered, Organisation, Society) instead of dimension
 * @param {HTMLElement} container - Container element for tooltip
 * @param {Object} riskData - Risk assessment data
 */
function buildTooltip(container, riskData) {
    const sections = [
        {
            title: 'Konsekvens for den registrerede',
            values: {
                'Fortrolighed': riskData.avgConsequenceConfidentialityRegistered,
                'Integritet': riskData.avgConsequenceIntegrityRegistered,
                'Tilgængelighed': riskData.avgConsequenceAvailabilityRegistered
            }
        },
        {
            title: 'Konsekvens for organisationen',
            values: {
                'Fortrolighed': riskData.avgConsequenceConfidentialityOrganisation,
                'Integritet': riskData.avgConsequenceIntegrityOrganisation,
                'Tilgængelighed': riskData.avgConsequenceAvailabilityOrganisation
            }
        },
        {
            title: 'Konsekvens for samfundet',
            values: {
                'Fortrolighed': riskData.avgConsequenceConfidentialitySociety,
                'Integritet': riskData.avgConsequenceIntegritySociety,
                'Tilgængelighed': riskData.avgConsequenceAvailabilitySociety,
                'Autenticitet': riskData.avgConsequenceAuthenticitySociety
            }
        }
    ];

    sections.forEach(section => {
        if (hasAnyValue(...Object.values(section.values))) {
            addTooltipSection(container, section.title, section.values);
        }
    });
}

/**
 * Adds a section to the tooltip
 * @param {HTMLElement} container - Tooltip container
 * @param {string} title - Section title
 * @param {Object} values - Values to display
 */
function addTooltipSection(container, title, values) {
    const section = templates.tooltipSection.content.cloneNode(true);
    const titleElement = section.querySelector('[data-section-title]');
    const rowsContainer = section.querySelector('[data-section-rows]');

    titleElement.textContent = title;

    Object.entries(values)
        .filter(([_, value]) => value !== null && value !== undefined)
        .forEach(([label, value]) => {
            const row = templates.tooltipRow.content.cloneNode(true);
            const labelElement = row.querySelector('[data-row-label]');
            const valueElement = row.querySelector('[data-row-value]');

            labelElement.textContent = label + ':';
            valueElement.textContent = value.toFixed(1);
            valueElement.classList.add(getRiskColorClass(value));

            rowsContainer.appendChild(row);
        });

    container.appendChild(section);
}

/**
 * Helper to check if any value exists
 * @param {...*} values - Values to check
 * @returns {boolean} True if any value is not null/undefined
 */
function hasAnyValue(...values) {
    return values.some(v => v !== null && v !== undefined && v !== 0);
}