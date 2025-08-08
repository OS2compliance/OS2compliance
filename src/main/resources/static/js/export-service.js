/**
 * Export grid data to Excel by calling the existing list endpoint with export=true
 * @param {string} listEndpoint - The existing list endpoint URL (e.g., '/register/list')
 * @param {Object} currentFilters - Current filters applied to the grid
 * @param {string} sortColumn - Current sort column
 * @param {string} sortDirection - Current sort direction ('ASC' or 'DESC')
 * @param {string} fileName - Optional filename for the export
 */
async function exportGridToExcel(listEndpoint, currentFilters = {}, sortColumn = null,
                                 sortDirection = 'ASC', fileName = 'export.xlsx') {
    try {
        // Build form data with all current parameters plus export=true
        const formData = new FormData();

        // Add export flag
        formData.append('export', 'true');
        formData.append('fileName', fileName);

        // Add filters
        Object.keys(currentFilters).forEach(key => {
            if (currentFilters[key] !== null && currentFilters[key] !== undefined && currentFilters[key] !== '') {
                formData.append(key, currentFilters[key]);
            }
        });

        // Add sorting
        if (sortColumn) {
            formData.append('order', sortColumn);
        }
        formData.append('dir', sortDirection);

        console.log(formData);

        // Make request to the SAME endpoint that feeds the grid
        const response = await fetch(listEndpoint, {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            body: formData
        });

        if (!response.ok) {
            throw new Error(`Export failed: ${response.status} ${response.statusText}`);
        }

        // Check if response is Excel file or JSON error
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            // It's JSON, probably an error
            const errorData = await response.json();
            throw new Error(errorData.message || 'Export failed');
        }

        // It's an Excel file, download it
        const blob = await response.blob();
        downloadBlob(blob, fileName);

        console.log('Export completed successfully');

    } catch (error) {
        console.error('Export failed:', error);
        alert('Export failed. Please try again.');
    }
}

/**
 * Download a blob as a file
 */
function downloadBlob(blob, fileName) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
}

/**
 * Helper function to get current filters from a grid
 * TODO: Implement the rest
 */
function getCurrentGridFilters(gridId) {
    const filters = {};

    const gridContainer = document.getElementById(gridId);
    if (gridContainer) {
        // Get from search inputs
        const searchInputs = gridContainer.querySelectorAll('[data-filter], .grid-filter');
        searchInputs.forEach(input => {
            const filterName = input.dataset.filter || input.name;
            if (input.value && filterName) {
                filters[filterName] = input.value;
            }
        });

        // Get from select dropdowns
        const selectFilters = gridContainer.querySelectorAll('select[data-filter]');
        selectFilters.forEach(select => {
            const filterName = select.dataset.filter;
            if (select.value && filterName) {
                filters[filterName] = select.value;
            }
        });
    }

    return filters;
}

/**
 * Helper function to get current sort from a grid
 * TODO: Complete implementation
 */
function getCurrentGridSort(gridId) {
    const gridContainer = document.getElementById(gridId);
    if (gridContainer) {
        return {
            column: gridContainer.dataset.sortColumn || null,
            direction: gridContainer.dataset.sortDirection || 'ASC'
        };
    }

    return {
        column: null,
        direction: 'ASC'
    };
}

/**
 * Convenience functions for specific tables
 */
function exportRegistersToExcel() {
    const filters = getCurrentGridFilters('register-grid');
    const sort = getCurrentGridSort('register-grid');

    exportGridToExcel(
        '/register/list',
        filters,
        sort.column,
        sort.direction,
        'registers_export.xlsx'
    );
}

function exportUsersToExcel() {
    const filters = getCurrentGridFilters('users-grid');
    const sort = getCurrentGridSort('users-grid');

    exportGridToExcel(
        '/users/list',
        filters,
        sort.column,
        sort.direction,
        'users_export.xlsx'
    );
}

/**
 * Generic export function that can be used with any grid
 */
function exportAnyGrid(listEndpoint, gridId, fileName) {
    const filters = getCurrentGridFilters(gridId);
    const sort = getCurrentGridSort(gridId);

    exportGridToExcel(listEndpoint, filters, sort.column, sort.direction, fileName);
}