export { exportGridServerSide, exportHtmlTableToExcel, downloadBlob };

/**
 * Download a blob as a file
 */
function downloadBlob(blob, fileName) {
    const today = new Date();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName + '_' + today.getFullYear() + '-' + (today.getMonth()+1) + '-' + today.getDate() + '.xlsx';
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
}

/**
 * Exports a serverside grid table to excel
 */
async function exportGridToExcelSheet(customGridInstance, fileName = 'export.xlsx') {
    if (!customGridInstance) {
        return;
    }

    try {
        const exportUrl = customGridInstance.getExportUrl();
        const url = new URL(exportUrl, window.location.origin);
        url.searchParams.set('fileName', fileName);

        const response = await fetch(url.toString(), {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            }
        });

        if (!response.ok) {
            toastService.error("Export fejlede. Tjek at tabellen ikke er tomt.");
            return;
        }

        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Export failed');
        }

        const blob = await response.blob();
        downloadBlob(blob, fileName);
    } catch (error) {
        toastService.error("Export fejlede. Prøv igen.");
    }
}

/**
 * Generic export function that works with any CustomGridFunctions instance
 */
function exportGridServerSide(customGridInstance, fileName = 'export.xlsx') {
    exportGridToExcelSheet(customGridInstance, fileName);
}

/**
 * Generic export function for frontend tables
 */
async function exportHtmlTableToExcel(tableId, fileName = "export.xlsx") {
    const table = document.getElementById(tableId);
    if (!table) {
        return;
    }

    const columns = Array.from(table.querySelectorAll("thead th")).map(th => th.textContent.trim());
    const rows = Array.from(table.querySelectorAll("tbody tr")).map(tr => {
        return Array.from(tr.querySelectorAll("td")).map(td => td.textContent.trim());
    });

    const response = await fetch('/export-excel', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ columns, rows })
    });

    const blob = await response.blob();
    downloadBlob(blob, fileName);
}