 async function exportGridToExcel(gridInstance, fileName = "export.xlsx") {
    const table = document.getElementById(tableId);
    if (!table) {
        console.error(`Table with ID '${tableId}' not found.`);
        return;
    }

    gridInstance.



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
        body: JSON.stringify({
            columns,
            rows
        })
    });

    // Receive file blob and trigger download
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
}
