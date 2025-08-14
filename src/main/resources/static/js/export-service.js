/**
 * Exports a serverside grid table to excel, i.e., finds the filters, sortings etc. and then calls an endpoint that retrieves the data from the DB
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 * @param {string} fileName - Optional filename for the export
 */
async function exportGridToExcelSheet(customGridInstance, fileName = 'export.xlsx') {
    if (!customGridInstance) {
        console.error('CustomGridFunctions instance is required');
        return;
    }

    try {
        // Build the export URL
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
    const today = new Date();

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName + '_' + today.getFullYear() + '-' + (today.getMonth()+1) + '-' + today.getDate().toString()
        + '.xlsx';
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
}

/**
 * Generic export function that works with any CustomGridFunctions instance
 * @param {CustomGridFunctions} customGridInstance - The CustomGridFunctions instance
 * @param {string} fileName - Optional filename override
 */
function exportGridServerSide(customGridInstance, fileName = 'export.xlsx') {
    exportGridToExcelSheet(customGridInstance, fileName);
}

/**
 * Generic export function that works for frontend tables, i.e., tables where we simply export what's shown in the html page.
 * @param tableId - The id of the grid table we want to export.
 * @param fileName - The file name we want the exported sheet to have.
 * @returns {Promise<void>}
 */
async function exportHtmlTableToExcel(tableId, fileName = "export.xlsx") {
    const table = document.getElementById(tableId);
    if (!table) {
        console.error(`Table with ID '${tableId}' not found.`);
        return;
    }

    // Get column headers
    const columns = Array.from(table.querySelectorAll("thead th")).map(th => th.textContent.trim());

    // Get table rows
    const rows = Array.from(table.querySelectorAll("tbody tr")).map(tr => {
        return Array.from(tr.querySelectorAll("td")).map(td => td.textContent.trim());
    });

    // Send to backend
    const response = await fetch('/export-excel', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ columns, rows })
    });

    const today = new Date();

    // Receive file blob and trigger download
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName + '_' + today.getFullYear() + '-' + (today.getMonth()+1) + '-' + today.getDate().toString()
        + '.xlsx';
    document.body.appendChild(a);
    a.click();
    a.remove();
}