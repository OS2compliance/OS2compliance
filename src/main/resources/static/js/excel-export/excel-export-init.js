import { ExcelExportDialog } from './excel-export-dialog.js';

/**
 * Initialize Excel export button for serverside grid tables
 */
export function initSaveAsExcelButton(customGridFunctions, entityType, urlName, defaultFileName, extraFiltersFunction = null) {
    const button = document.getElementById('saveAsExcelButton');
    if (!button) {
        return;
    }

    button.addEventListener('click', async () => {
        const mode = customGridFunctions ? 'serverside' : 'clientside';

        // Merge grid filters with extra filters
        let filters = customGridFunctions ? customGridFunctions.getFilters() : {};
        if (extraFiltersFunction && typeof extraFiltersFunction === 'function') {
            const extraFilters = extraFiltersFunction();
            filters = { ...filters, ...extraFilters };
        }

        const config = {
            mode: mode,
            entityType: entityType,
            metadataUrl: `/rest/${urlName}/export-metadata`,
            entitiesUrl: `/rest/${urlName}/export-entities`,
            exportUrl: `/rest/${urlName}/export-custom`,
            customGridFunctions: customGridFunctions,
            defaultFileName: defaultFileName
        };

        const dialog = new ExcelExportDialog(config);

        // Store extra filters for use in dialog
        if (extraFiltersFunction) {
            dialog.extraFilters = extraFiltersFunction();
        }

        await dialog.show();
    });
}

/**
 * Initialize Excel export button for clientside HTML tables
 */
export function initSaveAsExcelButtonClientside(tableId, entityType, urlName, filename) {
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