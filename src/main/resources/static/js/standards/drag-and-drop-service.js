/**
 * Reusable drag-and-drop row reordering service for grouped tables.
 *
 * ## How to use
 *
 * 1. Mark each draggable row with the CSS class passed as `rowClass` and set:
 *      - `draggable="true"` on the <tr>
 *      - `data-parent="<groupKey>"` — rows with the same value can be reordered
 *        relative to each other; rows in different groups cannot be swapped
 *      - `data-identifier="<id>"` — the value sent to the server for each row
 *
 * 2. Each draggable row must be followed immediately by exactly two sibling rows
 *    that move with it (e.g. a detail/expand row and a divider row). These are
 *    picked up automatically — no extra markup needed.
 *
 * 3. Call `dragAndDropService.init(options)` once after the DOM is ready.
 *
 * ## Options
 *
 * @param {object}   options
 * @param {string}   options.rowClass   CSS class on each draggable <tr> (without the dot)
 * @param {function} options.onReorder  Called after a successful DOM reorder.
 *                                      Receives `(identifiers, draggedRow)` where
 *                                      `identifiers` is the ordered array of
 *                                      `data-identifier` values for the affected group.
 *
 * ## Minimal example
 *
 * HTML:
 *   <tr class="my-row" draggable="true" data-parent="group-1" data-identifier="42">
 *     <td>Row label</td>
 *     ...
 *   </tr>
 *
 * JS:
 *   dragAndDropService.init({
 *     rowClass: 'my-row',
 *     onReorder: (identifiers, draggedRow) => {
 *       postData('/api/reorder', identifiers).then(r => {
 *         if (!r.ok) toastService.error('Could not save order');
 *       }).catch(() => toastService.error('Could not save order'));
 *     }
 *   });
 */
const dragAndDropService = (() => {
    function init({ rowClass, onReorder }) {
        let draggedRow = null;

        document.addEventListener('dragstart', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) return;
            draggedRow = row;
            setTimeout(() => row.classList.add('dragging'), 0);
        });

        document.addEventListener('dragend', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) return;
            row.classList.remove('dragging');
            document.querySelectorAll('.' + rowClass).forEach(r => r.classList.remove('drag-over'));
            draggedRow = null;
        });

        document.addEventListener('dragover', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row || !draggedRow || draggedRow === row) return;

            if (draggedRow.dataset.parent !== row.dataset.parent) {
                e.dataTransfer.dropEffect = 'none';
                return;
            }

            e.preventDefault();
            document.querySelectorAll('.' + rowClass).forEach(r => r.classList.remove('drag-over'));
            row.classList.add('drag-over');
        });

        document.addEventListener('drop', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) return;
            e.preventDefault();
            if (!draggedRow || draggedRow === row) return;
            if (draggedRow.dataset.parent !== row.dataset.parent) return;

            const tbody = row.closest('tbody');
            if (!tbody) return;

            const allRows = [...tbody.querySelectorAll('.' + rowClass)];
            const draggedIndex = allRows.indexOf(draggedRow);
            const targetIndex = allRows.indexOf(row);

            const getRowGroup = (r) => [r, r.nextElementSibling, r.nextElementSibling?.nextElementSibling].filter(Boolean);

            if (draggedIndex < targetIndex) {
                const targetGroup = getRowGroup(row);
                targetGroup[targetGroup.length - 1].after(...getRowGroup(draggedRow));
            } else {
                row.before(...getRowGroup(draggedRow));
            }

            const groupRows = [...tbody.querySelectorAll('.' + rowClass + '[data-parent="' + draggedRow.dataset.parent + '"]')];
            const identifiers = groupRows.map(r => r.dataset.identifier);

            onReorder(identifiers, draggedRow);
        });
    }

    return { init };
})();
