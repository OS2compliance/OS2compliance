/**
 * Reusable drag-and-drop row reordering for grouped tables.
 * Rows need: draggable="true", data-parent="<groupKey>", data-identifier="<id>".
 * Each draggable row must be followed by exactly two sibling rows that move with it.
 * Call dragAndDropService.init({ rowClass, onReorder }) once after DOMContentLoaded.
 * onReorder receives (identifiers, draggedRow) where identifiers is the new ordered
 * array of data-identifier values for the affected group.
 */
const dragAndDropService = (() => {
    function init({ rowClass, onReorder }) {
        let draggedRow = null;

        document.addEventListener('dragstart', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) {
                return;
            }
            draggedRow = row;
            setTimeout(() => row.classList.add('dragging'), 0);
        });

        document.addEventListener('dragend', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) {
                return;
            }
            row.classList.remove('dragging');
            document.querySelectorAll('.' + rowClass).forEach(r => {
                r.classList.remove('drag-over-above');
                r.classList.remove('drag-over-below');
            });
            draggedRow = null;
        });

        document.addEventListener('dragover', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row || !draggedRow || draggedRow === row) {
                return;
            }

            if (draggedRow.dataset.parent !== row.dataset.parent) {
                e.dataTransfer.dropEffect = 'none';
                return;
            }

            e.preventDefault();
            document.querySelectorAll('.' + rowClass).forEach(r => {
                r.classList.remove('drag-over-above');
                r.classList.remove('drag-over-below');
            });

            const allRows = [...row.closest('tbody').querySelectorAll('.' + rowClass)];
            const draggedIndex = allRows.indexOf(draggedRow);
            const targetIndex = allRows.indexOf(row);

            if (draggedIndex < targetIndex) {
                row.classList.add('drag-over-below');
            } else {
                row.classList.add('drag-over-above');
            }
        });

        document.addEventListener('drop', (e) => {
            const row = e.target.closest('.' + rowClass);
            if (!row) {
                return;
            }
            e.preventDefault();
            if (!draggedRow || draggedRow === row) {
                return;
            }
            if (draggedRow.dataset.parent !== row.dataset.parent) {
                return;
            }

            const tbody = row.closest('tbody');
            if (!tbody) {
                return;
            }

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
