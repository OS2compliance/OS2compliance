export { downloadBlob };

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