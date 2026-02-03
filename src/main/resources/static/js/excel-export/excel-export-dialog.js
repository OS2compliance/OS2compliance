import { downloadBlob } from './excel-export-utils.js';

export class ExcelExportDialog {

    /**
     * Creates a new Excel Export Dialog
     * @param {Object} config - Configuration object
     * @param {string} config.mode - 'serverside' or 'clientside'
     * @param {string} config.entityType - Entity type identifier (e.g., 'documents')
     * @param {string} config.metadataUrl - API endpoint for column metadata
     * @param {string} config.entitiesUrl - API endpoint for entity list
     * @param {string} config.exportUrl - API endpoint for custom export
     * @param {CustomGridFunctions} [config.customGridFunctions] - Grid instance (serverside only)
     * @param {string} [config.tableId] - Table DOM ID (clientside only)
     * @param {string} config.defaultFileName - Default export filename
     */
    constructor(config) {
        this.mode = config.mode; // 'serverside' or 'clientside'
        this.entityType = config.entityType;
        this.metadataUrl = config.metadataUrl;
        this.entitiesUrl = config.entitiesUrl;
        this.exportUrl = config.exportUrl;
        this.customGridFunctions = config.customGridFunctions;
        this.tableId = config.tableId;
        this.defaultFileName = config.defaultFileName;

        this.choicesInstance = null;
        this.selectedEntities = [];
        this.allEntities = [];
        this.availableColumns = [];
        this.modalElement = null;
        this.modalInstance = null;
    }

    async show() {
        this.modalElement = document.getElementById('excelExportModal');
        if (!this.modalElement) {
            return;
        }

        this.modalInstance = new bootstrap.Modal(this.modalElement);
        this.modalInstance.show();

        // Show loading state
        this.showLoading(true);

        try {
            await this.loadMetadata();
            await this.loadDefaultEntities();

            // Check if there are any entities to export
            if (this.allEntities.length === 0) {
                this.modalInstance.hide();
                toastService.error('Der er ingen objekter at eksportere');
                return;
            }

            this.populateModal();
            this.initializeChoices();
            this.attachEventListeners();

            // Hide loading, show content
            this.showLoading(false);
        } catch (error) {
            this.modalInstance.hide();
            toastService.error('Kunne ikke indlæse eksport dialog. Prøv igen.');
        }

        // Add cleanup listener
        this.modalElement.addEventListener('hidden.bs.modal', () => {
            this.cleanup();
        }, { once: true });
    }

    async loadDefaultEntities() {
        if (this.mode === 'serverside') {
            const filters = this.customGridFunctions.getFilters();

            // Merge with extra filters if available
            const allFilters = this.extraFilters
                ? { ...filters, ...this.extraFilters }
                : filters;

            let sortState = { column: null, direction: 'ASC' };
            if (this.customGridFunctions && typeof this.customGridFunctions.getSortState === 'function') {
                sortState = this.customGridFunctions.getSortState();
            }

            // Fetch ALL available entities
            const allEntitiesResponse = await fetch(this.entitiesUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({
                    filters: this.extraFilters || {},
                    sortColumn: sortState.column,
                    sortDirection: sortState.direction
                })
            });

            if (!allEntitiesResponse.ok) {
                throw new Error('Failed to load all entities');
            }

            this.allEntities = await allEntitiesResponse.json();

            // Fetch filtered entities
            const filteredEntitiesResponse = await fetch(this.entitiesUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({
                    filters: allFilters,
                    sortColumn: sortState.column,
                    sortDirection: sortState.direction
                })
            });

            if (!filteredEntitiesResponse.ok) {
                throw new Error('Failed to load filtered entities');
            }

            this.selectedEntities = await filteredEntitiesResponse.json();
        } else {
             // Clientside: extract from table DOM
            const table = document.getElementById(this.tableId);
            const rows = Array.from(table.querySelectorAll('tbody tr'));
            this.selectedEntities = rows.map(row => ({
                id: row.dataset.id || row.querySelector('td:first-child')?.textContent.trim(),
                name: row.querySelector('td:first-child')?.textContent.trim()
            }));
            this.allEntities = this.selectedEntities;
        }
    }

    async loadMetadata() {
        const response = await fetch(this.metadataUrl, {
            headers: { 'X-CSRF-TOKEN': token }
        });

        if (!response.ok) {
            throw new Error('Failed to load metadata');
        }

        const metadata = await response.json();
        this.availableColumns = metadata.availableColumns;
        this.specialColumns = metadata.specialColumns || [];
    }

    populateModal() {
        // Update entity count label
        const countLabel = document.getElementById('entityCountLabel');
        countLabel.textContent = `(${this.selectedEntities.length} valgt som standard)`;

        // Set filename
        const fileNameInput = document.getElementById('excelFileName');
        fileNameInput.value = this.defaultFileName;

        // Render column checkboxes
        this.renderColumnCheckboxes();
    }

    renderColumnCheckboxes() {
        const container = document.getElementById('columnCheckboxes');
        container.replaceChildren();

        const template = document.getElementById('columnCheckboxTemplate');

        // Regular columns
        this.availableColumns.forEach(col => {
            const clone = template.content.cloneNode(true);

            const checkbox = clone.querySelector('.column-checkbox');
            checkbox.id = `col_${col.fieldName}`;
            checkbox.value = col.fieldName;
            checkbox.checked = true;

            const label = clone.querySelector('.form-check-label');
            label.htmlFor = checkbox.id;
            label.textContent = col.displayName;

            container.appendChild(clone);
        });

        // Special columns
        if (this.specialColumns.length > 0) {
            const hr = document.createElement('hr');
            container.appendChild(hr);

            const small = document.createElement('small');
            small.className = 'text-muted';
            small.textContent = 'Ekstra felter:';
            container.appendChild(small);

            this.specialColumns.forEach(col => {
                const clone = template.content.cloneNode(true);

                const checkbox = clone.querySelector('.column-checkbox');
                checkbox.id = `col_${col.fieldName}`;
                checkbox.value = col.fieldName;
                checkbox.checked = false;

                const label = clone.querySelector('.form-check-label');
                label.htmlFor = checkbox.id;
                label.textContent = col.displayName;

                container.appendChild(clone);
            });
        }
    }

    initializeChoices() {
        const selectElement = document.getElementById('entitySelect');

        // Destroy existing Choices instance if it exists
        if (this.choicesInstance) {
            this.choicesInstance.destroy();
            this.choicesInstance = null;
        }

        // Clear all existing options using modern API
        selectElement.replaceChildren();

        this.choicesInstance = new Choices(selectElement, {
            removeItemButton: true,
            searchEnabled: true,
            searchPlaceholderValue: 'Søg...',
            noResultsText: 'Ingen resultater fundet',
            itemSelectText: 'Tryk for at vælge',
            maxItemCount: -1,
            shouldSort: false
        });

        // Create Set of selected IDs for quick lookup
        const selectedIds = new Set(this.selectedEntities.map(e => String(e.id)));

        // Use ALL entities as choices, but only pre-select the filtered ones
        const choices = this.allEntities.map(entity => ({
            value: String(entity.id),
            label: entity.name,
            selected: selectedIds.has(String(entity.id))
        }));

        this.choicesInstance.setChoices(choices, 'value', 'label', true);
    }

    attachEventListeners() {
        // Select all columns checkbox
        const selectAllCheckbox = document.getElementById('selectAllColumns');
        const selectAllHandler = (e) => {
            const checkboxes = document.querySelectorAll('.column-checkbox');
            checkboxes.forEach(cb => cb.checked = e.target.checked);
        };
        selectAllCheckbox.addEventListener('change', selectAllHandler);

        // Store handler so we can remove it later
        this.selectAllHandler = selectAllHandler;

        // Individual column checkboxes update "select all"
        const columnCheckboxes = document.querySelectorAll('.column-checkbox');
        const columnChangeHandler = () => {
            const allCheckboxes = document.querySelectorAll('.column-checkbox');
            const allChecked = Array.from(allCheckboxes).every(checkbox => checkbox.checked);
            selectAllCheckbox.checked = allChecked;
        };

        columnCheckboxes.forEach(cb => {
            cb.addEventListener('change', columnChangeHandler);
        });

        // Store handlers
        this.columnChangeHandler = columnChangeHandler;
        this.columnCheckboxes = Array.from(columnCheckboxes);

        // Export button
        const exportButton = document.getElementById('exportButton');
        const exportHandler = () => {
            this.performExport();
        };
        exportButton.addEventListener('click', exportHandler);

        // Store handler so we can remove it later
        this.exportHandler = exportHandler;
    }

    async performExport() {
        const selectedEntityIds = this.choicesInstance.getValue(true);
        const selectedColumns = Array.from(document.querySelectorAll('.column-checkbox:checked'))
            .map(cb => cb.value);
        const fileName = document.getElementById('excelFileName').value || this.defaultFileName;

        // Safely get sort state with fallback
        let sortState = { column: null, direction: 'ASC' };
        if (this.customGridFunctions && typeof this.customGridFunctions.getSortState === 'function') {
            sortState = this.customGridFunctions.getSortState();
        }

        if (selectedEntityIds.length === 0) {
            toastService.error('Vælg mindst én post at eksportere');
            return;
        }

        if (selectedColumns.length === 0) {
            toastService.error('Vælg mindst én kolonne at eksportere');
            return;
        }

        const exportButton = document.getElementById('exportButton');
        const buttonText = document.getElementById('exportButtonText');
        const buttonSpinner = document.getElementById('exportButtonSpinner');
        const buttonIcon = exportButton.querySelector('.fa-download');

        // Set loading state
        exportButton.disabled = true;
        buttonIcon.style.display = 'none';
        buttonText.textContent = 'Eksporterer...';
        buttonSpinner.style.display = 'inline-block';

        try {
            const response = await fetch(this.exportUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': token
                },
                body: JSON.stringify({
                    selectedIds: selectedEntityIds,
                    selectedColumns: selectedColumns,
                    fileName: fileName,
                    sortColumn: sortState.column,
                    sortDirection: sortState.direction
                })
            });

            if (!response.ok) {
                throw new Error('Export fejlede');
            }

            const blob = await response.blob();
            downloadBlob(blob, fileName);

            toastService.info('Excel fil downloadet');

            // Close modal AFTER everything is done
            this.modalInstance.hide();

        } catch (error) {
            toastService.error('Eksport fejlede. Prøv igen.');

            // Reset button state only on error
            exportButton.disabled = false;
            buttonIcon.style.display = '';
            buttonText.textContent = 'Eksporter';
            buttonSpinner.style.display = 'none';
        }
    }

    cleanup() {
        // Destroy Choices instance
        if (this.choicesInstance) {
            this.choicesInstance.destroy();
            this.choicesInstance = null;
        }

        // Remove event listeners
        const selectAllCheckbox = document.getElementById('selectAllColumns');
        if (selectAllCheckbox && this.selectAllHandler) {
            selectAllCheckbox.removeEventListener('change', this.selectAllHandler);
        }

        if (this.columnCheckboxes && this.columnChangeHandler) {
            this.columnCheckboxes.forEach(cb => {
                cb.removeEventListener('change', this.columnChangeHandler);
            });
        }

        const exportButton = document.getElementById('exportButton');
        if (exportButton && this.exportHandler) {
            exportButton.removeEventListener('click', this.exportHandler);
        }

        // Reset export button state
        const buttonText = document.getElementById('exportButtonText');
        const buttonSpinner = document.getElementById('exportButtonSpinner');
        const buttonIcon = exportButton?.querySelector('.fa-download');

        if (exportButton && buttonText && buttonSpinner && buttonIcon) {
            exportButton.disabled = false;
            buttonIcon.style.display = '';
            buttonText.textContent = 'Eksporter';
            buttonSpinner.style.display = 'none';
        }

        // Clear modal instance
        this.modalInstance = null;
    }

    showLoading(loading) {
        const loadingState = document.getElementById('modalLoadingState');
        const mainContent = document.getElementById('modalMainContent');

        if (loading) {
            loadingState.style.display = 'block';
            mainContent.style.display = 'none';
        } else {
            loadingState.style.display = 'none';
            mainContent.style.display = 'block';
        }
    }
}