/**
 * Positions risk assessment tooltips dynamically based on available space
 */

document.addEventListener('DOMContentLoaded', function() {
    // Use event delegation since cells are rendered by GridJS
    document.addEventListener('mouseenter', function(e) {
        const cell = e.target.closest('.risk-assessment-cell');
        if (!cell) return;

        const tooltip = cell.querySelector('.risk-tooltip');
        if (!tooltip) return;

        positionTooltip(cell, tooltip);
    }, true);
});

/**
 * Positions tooltip above or below cell based on available viewport space
 */
function positionTooltip(cell, tooltip) {
    const cellRect = cell.getBoundingClientRect();
    const tooltipRect = tooltip.getBoundingClientRect();

    const spaceAbove = cellRect.top;
    const spaceBelow = window.innerHeight - cellRect.bottom;
    const tooltipHeight = tooltipRect.height || 200; // Estimate if not rendered yet

    const gap = 8; // Space between cell and tooltip

    // Decide whether to show above or below
    if (spaceAbove > tooltipHeight + gap || spaceAbove > spaceBelow) {
        // Show above
        tooltip.style.top = (cellRect.top - tooltipHeight - gap) + 'px';
        tooltip.setAttribute('data-position', 'top');
    } else {
        // Show below
        tooltip.style.top = (cellRect.bottom + gap) + 'px';
        tooltip.setAttribute('data-position', 'bottom');
    }

    // Center horizontally
    tooltip.style.left = (cellRect.left + cellRect.width / 2) + 'px';
    tooltip.style.transform = 'translateX(-50%)';
}