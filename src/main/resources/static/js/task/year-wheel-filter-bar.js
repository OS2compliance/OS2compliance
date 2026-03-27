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

var pillTemplate = null;

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
        bar.classList.add('yw-hidden');
        return;
    }

    bar.classList.remove('yw-hidden');

    if (!pillTemplate) {
        pillTemplate = document.getElementById('tmplFilterPill');
    }

    activeFilters.forEach(function (entry) {
        var key = entry[0];
        var value = entry[1];

        var fragment = pillTemplate.content.cloneNode(true);
        fragment.querySelector('[data-ref="label"]').textContent = FILTER_LABELS[key] || key;
        fragment.querySelector('[data-ref="value"]').textContent = ': ' + (FILTER_VALUE_LABELS[value] || value);

        pills.appendChild(fragment);
    });
}