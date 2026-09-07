/**
 * Reads back a date the grid state holds in the format the datepicker writes it, so the picker can be
 * restored from the same value the server filters on.
 */
function parseDkDate(value) {
    const parts = /^(\d{2})\/(\d{2})-(\d{4})$/.exec(value || '');
    return parts ? new Date(Number(parts[3]), Number(parts[2]) - 1, Number(parts[1])) : null;
}

/**
 * Wires the "Fra"/"Til" date inputs and the date-field select to a grid's own search state, the same
 * way incident-grid-service.js does it for the incidents log. Filter values are persisted by
 * CustomGridFunctions itself, so there is no separate storage to keep in step.
 */
export function initTaskDateFilter(customGridFunctions, { fromInput, fromBtn, toInput, toBtn, dateFieldSelect }) {
    const filterValue = (key) => customGridFunctions.state.searchValues[key] || '';
    const setFilter = (key, value) => {
        customGridFunctions.updateColumnValue(key, value || '');
        customGridFunctions.saveState();
        customGridFunctions.onSearch();
    };

    initDatePicker(fromBtn, fromInput, 'fromDate', filterValue, setFilter);
    initDatePicker(toBtn, toInput, 'toDate', filterValue, setFilter);

    dateFieldSelect.value = filterValue('dateField') || 'DEADLINE';
    dateFieldSelect.addEventListener('change', (event) => setFilter('dateField', event.target.value));
}

function initDatePicker(buttonSelector, inputSelector, filterKey, filterValue, setFilter) {
    const picker = initDatepicker(buttonSelector, inputSelector);
    const saved = parseDkDate(filterValue(filterKey));
    if (saved) {
        picker.setFullDate(saved);
    }
    picker.onSelect((date, formatedDate) => setFilter(filterKey, formatedDate));
    // "Ryd" empties the input without firing onSelect, so without this the box goes blank while the
    // grid keeps filtering on the old date.
    picker.onClear(() => setFilter(filterKey, ''));
}
