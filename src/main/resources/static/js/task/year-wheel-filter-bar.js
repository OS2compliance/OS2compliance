var FILTER_LABELS = {
    'taskType': 'Opgave type',
    'taskRepetition': 'Gentagelse',
    'name': 'Opgavenavn',
    'responsibleNames': 'Ansvarlig',
    'responsibleOU.name': 'Afdeling',
    'tagNames': 'Tags'
};

var FILTER_VALUE_LABELS = {
    'CHECK': 'Kontrol',
    'TASK': 'Opgave',
    'MONTHLY': 'Månedligt',
    'QUARTERLY': 'Kvartalsvis',
    'EVERY_2_MONTHS': 'Hver anden måned',
    'EVERY_3_MONTHS': 'Hver tredje måned',
    'EVERY_4_MONTHS': 'Hver fjerde måned',
    'HALF_YEARLY': 'Halvårligt',
    'YEARLY': 'Årligt',
    'EVERY_SECOND_YEAR': 'Hvert 2. år',
    'EVERY_THIRD_YEAR': 'Hvert 3. år'
};

/**
 * Updates the filter indicator bar to show which filters are currently active.
 * Display-only — filters are managed from the table view.
 * @param {Object} filters - The currently active filters from collectFilters()
 */
export function updateFilterBar(filters) {
    var bar = document.getElementById('yearWheelFilterBar');
    var pills = document.getElementById('yearWheelFilterPills');

    if (!bar || !pills) {
        return;
    }

    pills.innerHTML = '';

    var activeFilters = Object.entries(filters).filter(function ([key, value]) {
        return key !== 'onlyMine' && value && value.trim() !== '';
    });

    if (activeFilters.length === 0) {
        bar.style.display = 'none';
        return;
    }

    bar.style.display = 'flex';

    activeFilters.forEach(function (entry) {
        var key = entry[0];
        var value = entry[1];

        var pill = document.createElement('span');
        pill.className = 'filter-pill';

        var label = document.createElement('span');
        label.className = 'filter-pill-label';
        label.textContent = FILTER_LABELS[key] || key;

        var valueText = document.createTextNode(': ' + (FILTER_VALUE_LABELS[value] || value));

        pill.appendChild(label);
        pill.appendChild(valueText);
        pills.appendChild(pill);
    });
}