import { ExcelExportDialog } from './excel-export-dialog.js';

export { initSaveAsExcelButton, initSaveAsExcelButtonClientside };

/**
 * Initialize Excel export button for serverside grid tables
 */
function initSaveAsExcelButton(customGridFunctions, entityType, urlName, filename) {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    if (!saveAsExcelButton) {
        return;
    }

    saveAsExcelButton.addEventListener("click", async () => {
        const dialog = new ExcelExportDialog({
            mode: 'serverside',
            entityType: entityType,
            metadataUrl: `/rest/${urlName}/export-metadata`,
            entitiesUrl: `/rest/${urlName}/export-entities`,
            exportUrl: `/rest/${urlName}/export-custom`,
            customGridFunctions: customGridFunctions,
            defaultFileName: filename
        });

        await dialog.show();
    });
}

/**
 * Initialize Excel export button for clientside HTML tables
 */
function initSaveAsExcelButtonClientside(tableId, entityType, urlName, filename) {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    if (!saveAsExcelButton) {
        return;
    }

    saveAsExcelButton.addEventListener("click", async () => {
        const dialog = new ExcelExportDialog({
            mode: 'clientside',
            entityType: entityType,
            metadataUrl: `/rest/${urlName}/export-metadata`,
            entitiesUrl: null,
            exportUrl: `/rest/${urlName}/export-custom`,
            tableId: tableId,
            defaultFileName: filename
        });

        await dialog.show();
    });
}