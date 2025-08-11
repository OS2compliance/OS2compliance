/**
 * Export grid data to Excel using CustomGridFunctions instance
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 * @param {string} fileName - Optional filename for the export
 */
async function exportGridToExcelSimple(customGridInstance, fileName = 'export.xlsx') {
    if (!customGridInstance) {
        console.error('CustomGridFunctions instance is required');
        return;
    }

    try {
        // Build the export URL using the existing getExportUrl method
        const exportUrl = customGridInstance.getExportUrl();

        // Add export flag and filename to the URL
        const url = new URL(exportUrl, window.location.origin);
        url.searchParams.set('export', 'true');
        url.searchParams.set('fileName', fileName);

        // Make request to the endpoint
        const response = await fetch(url.toString(), {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            }
        });

        if (!response.ok) {
            throw new Error(`Export failed: ${response.status} ${response.statusText}`);
        }

        // Check if response is Excel file or JSON error
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Export failed');
        }

        // Download the Excel file
        const blob = await response.blob();
        downloadBlob(blob, fileName);
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
 * Helper function to get current filters from a CustomGridFunctions instance
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 */
function getCurrentGridFilters(customGridInstance) {
    if (!customGridInstance || !customGridInstance.state) {
        return {};
    }

    return customGridInstance.state.searchValues || {};
}

/**
 * Helper function to get current sort from a CustomGridFunctions instance
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 */
function getCurrentGridSort(customGridInstance) {
    if (!customGridInstance || !customGridInstance.state) {
        return {
            column: null,
            direction: 'ASC'
        };
    }

    return {
        column: customGridInstance.state.sortColumn || null,
        direction: customGridInstance.state.sortDirection || 'ASC'
    };
}

/**
 * Generic export function that works with any CustomGridFunctions instance
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 * @param {string} fileName - Optional filename override
 */
function exportGrid(customGridInstance, fileName = 'export.xlsx') {
    exportGridToExcelSimple(customGridInstance, fileName);
}

/**
 * Convenience functions for specific tables using CustomGridFunctions
 */
function exportRegistersToExcel(customGridInstance) {
    exportGrid(customGridInstance, 'registers_export.xlsx');
}

function exportUsersToExcel(customGridInstance) {
    exportGrid(customGridInstance, 'users_export.xlsx');
}