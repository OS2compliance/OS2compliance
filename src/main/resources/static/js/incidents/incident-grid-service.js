import ColumnOptions from "../grid-js-extension/column-options.js";
import IncidentService from "./incident-service.js";
import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";

// Custom incident fields are addressed by id, never by their column heading: the heading is free text
// an administrator can rename at any time, which would silently break every saved filter.
const FIELD_PREFIX = 'field_';

const DATE_FIELD_STORAGE_KEY = 'incidentDateField';
const SEARCH_DEBOUNCE_MS = 400;

export default function IncidentGridService () {
    this.incidentService = new IncidentService();

    this.filterFrom = '';
    this.filterTo = '';
    this.dateField = 'CREATED';
    this.customFields = [];
    this.dateFields = [];
    this.customGridFunctions = null;

    this.init = async () => {
        let fromPicker = initDatepicker('#filterFromBtn', '#filterFrom');
        let filterFrom = localStorage.getItem("incidentFilterFrom");
        if (filterFrom != null && filterFrom !== "null") {
            fromPicker.setFullDate(new Date(filterFrom));
            this.filterFrom = fromPicker.getFormatedDate();
        }
        fromPicker.onSelect((date, formatedDate) => this.setFilterFrom(date, formatedDate));
        // "Ryd" empties the input without firing onSelect, so without this the box goes blank while
        // the grid, the export and localStorage all keep filtering on the old date.
        fromPicker.onClear(() => this.setFilterFrom(null, ''));

        let toPicker = initDatepicker('#filterToBtn', '#filterTo');
        let filterTo = localStorage.getItem("incidentFilterTo");
        if (filterTo != null && filterTo !== "null") {
            toPicker.setFullDate(new Date(filterTo));
            this.filterTo = toPicker.getFormatedDate();
        }
        toPicker.onSelect((date, formatedDate) => this.setFilterTo(date, formatedDate));
        toPicker.onClear(() => this.setFilterTo(null, ''));

        this.dateField = localStorage.getItem(DATE_FIELD_STORAGE_KEY) || 'CREATED';

        this.customFields = await this.incidentService.fetchColumns() || [];
        this.dateFields = await this.incidentService.fetchDateFields() || [];
        // Resolve the saved date field before the first fetch, so a field that has since been removed
        // does not send the grid looking for answers to a field that no longer exists.
        this.initDateFieldSelect();
        this.initGrid();
    }

    this.generateExcel = () => {
        window.location.href = `/reports/incidents/excel?${this.reportQuery()}`;
    }

    this.generateReport = () => {
        fetch(`/reports/incidents?${this.reportQuery()}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`${response.status} ${response.statusText}`);
                }
                response.text()
                    .then(data => {
                        var win = window.open("", "Print Rapport", "height=600,width=800");
                        win.document.write(data);
                        win.print();
                    });
            })
            .catch(error => toastService.error(error));
    }

    /**
     * "Print rapport" and "Hent excel fil" cover the same date range as the grid, filtered on the same
     * date field. They deliberately ignore the free text search and the column filters — the reports
     * have always covered every incident within the range. "Gem som Excel" is the one that mirrors the
     * grid exactly, filters included.
     */
    this.reportQuery = () => {
        return new URLSearchParams({
            dateField: this.dateField,
            from: this.filterFrom,
            to: this.filterTo
        }).toString();
    }

    /**
     * Pushes the toolbar filters into the grid's search state and reloads. These are not column
     * filters, but they travel to the server the same way, which keeps them in the Excel export too.
     * <p>
     * The grid state is persisted, so on a plain reload it already holds these three values and there
     * is nothing to reload — the grid has fetched with them once already by the time we get here.
     */
    this.applyFilters = () => {
        if (!this.customGridFunctions) {
            return;
        }
        const saved = this.customGridFunctions.state.searchValues;
        const unchanged = (saved['fromDate'] || '') === this.filterFrom
            && (saved['toDate'] || '') === this.filterTo
            && (saved['dateField'] || 'CREATED') === this.dateField;

        this.customGridFunctions.updateColumnValue('fromDate', this.filterFrom);
        this.customGridFunctions.updateColumnValue('toDate', this.filterTo);
        this.customGridFunctions.updateColumnValue('dateField', this.dateField);
        if (unchanged) {
            return;
        }
        // Narrowing the result set while standing on page 4 would otherwise ask the server for a
        // page that no longer exists, and the grid would come back empty.
        this.customGridFunctions.state.page = 0;
        this.customGridFunctions.saveState();
        this.customGridFunctions.onSearch();
    }

    this.setFilterFrom = (date, formattedDate) => {
        this.filterFrom = formattedDate == null ? '' : formattedDate;
        localStorage.setItem("incidentFilterFrom", date);
        this.applyFilters();
    }

    this.setFilterTo = (date, formattedDate) => {
        this.filterTo = formattedDate == null ? '' : formattedDate;
        localStorage.setItem("incidentFilterTo", date);
        this.applyFilters();
    }

    /**
     * Fills the "Filtrer efter dato" picker with the two built-in timestamps plus the obligatory date
     * fields, so a setup that records an incident date can filter on that instead of on when the
     * incident happened to be typed in.
     */
    this.initDateFieldSelect = () => {
        const select = document.getElementById("dateFieldSelect");
        if (!select) {
            return;
        }

        for (const field of this.dateFields) {
            const option = document.createElement("option");
            option.value = FIELD_PREFIX + field.id;
            option.textContent = columnLabel(field);
            select.appendChild(option);
        }

        // A field can be removed or made optional after the choice was saved, so fall back to the default.
        if (Array.from(select.options).some(option => option.value === this.dateField)) {
            select.value = this.dateField;
        } else {
            this.dateField = 'CREATED';
            localStorage.removeItem(DATE_FIELD_STORAGE_KEY);
        }

        select.addEventListener("change", (event) => {
            this.dateField = event.target.value;
            localStorage.setItem(DATE_FIELD_STORAGE_KEY, this.dateField);
            this.applyFilters();
        });
    }

    /**
     * Free text across the title and every answer. GridJS' own search box is client side only and
     * CustomGridFunctions turns it off, so the input lives in the template instead.
     */
    this.initSearch = () => {
        const input = document.getElementById("incidentSearch");
        if (!input) {
            return;
        }
        input.value = this.customGridFunctions.state.searchValues['search'] || '';

        let debounce;
        input.addEventListener("input", (event) => {
            clearTimeout(debounce);
            const value = event.target.value;
            debounce = setTimeout(() => {
                this.customGridFunctions.updateColumnValue('search', value);
                this.customGridFunctions.state.page = 0;
                this.customGridFunctions.saveState();
                this.customGridFunctions.onSearch();
            }, SEARCH_DEBOUNCE_MS);
        });
    }

    this.initGrid = () => {
        const defaultClassName = {
            table: 'table table-striped',
            search: "form-control",
            header: "d-flex justify-content-end"
        };
        this.buildColumns();
        this.currentConfig = {
            className: defaultClassName,
            columns: this.columns,
            pagination: {
                limit: 50
            },
            language: {
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': () => 'hændelser',
                    'of': 'af',
                    'to': 'til'
                },
                'noRecordsFound': 'Ingen hændelser fundet'
            },
            server: {
                url: restUrl + 'list',
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => {
                    this.data = data;
                    return data.content.map(field => this.mapRow(field));
                },
                total: data => data.totalCount
            }
        };
        const datatableId = 'incidentsTable';
        this.incidentGrid = new gridjs.Grid(this.currentConfig);
        this.incidentGrid.render(document.getElementById(datatableId));

        this.customGridFunctions = new CustomGridFunctions(this.incidentGrid, restUrl + 'list', datatableId,
            {sortDirection: 'DESC', sortColumn: 'createdAt'});

        new ColumnOptions(
            datatableId,
            this.incidentGrid,
            ['name', 'allowedActions'],
            ['name', 'createdAt', 'updatedAt', 'allowedActions'],
            ['id', 'draft'])

        this.initSearch()
        this.initGridActions()
        initSaveAsExcelButton(this.customGridFunctions, 'incident', 'incidents', 'Hændelseslog');
        this.applyFilters();
    }

    this.mapRow = (field) => {
        // Answers are matched on the field id, not on the column heading, so renaming a heading does
        // not empty out the column.
        const columnValues = this.customFields.map(customField => {
            const response = field.responses.find(r => r.fieldId === customField.id);
            if (!response) {
                return "";
            }
            return response.linkable
                ? formatAsLink(response.answerValue, response.answerValue, true)
                : response.answerValue;
        });
        return [field.id, field.draft, field.name, field.createdAt, ...columnValues, field.updatedAt, field.allowedActions];
    }

    this.buildColumns = () => {
        const columns = [
            {
                id: "id",
                hidden: true,
            },
            {
                id: "draft",
                hidden: true,
            },
            {
                id: "name",
                name: "Titel",
                searchable: {
                    searchKey: 'name'
                },
                formatter: (cell, row) => {
                    const url = '/incidents/logs/' + row.cells[0]['data'];
                    const isDraft = row.cells[1]['data'];
                    return formatAsLink(cell, url, false, isDraft)
                },
                width: '250px'
            },
            {
                id: "createdAt",
                name: "Oprettet",
                width: '120px',
                searchable: {
                    searchKey: 'createdAt'
                }
            }
        ]

        this.customFields.forEach(field => {
            columns.push({
                // The column id stays the heading, because ColumnOptions keys saved column visibility
                // on it: switching to field_<id> would make every already saved incident log come back
                // with all custom columns hidden. Only the server contract uses the field id.
                id: columnLabel(field),
                name: columnLabel(field),
                searchable: {
                    searchKey: FIELD_PREFIX + field.id,
                    // Answers live in their own table and cannot be reached from a Pageable, so these
                    // columns filter but do not sort.
                    sortKey: null
                }
            })
        });
        columns.push(
            {
                id: "updatedAt",
                name: "Opdateret",
                searchable: {
                    searchKey: 'updatedAt'
                }
            }
        )
        columns.push(
            {
                id: "allowedActions",
                name: "Handlinger",
                sort: false,
                width: '90px',
                formatter: (cell, row) => {
                    const identifier = row.cells[0]['data'];
                    const name = row.cells[2]['data'].replaceAll("'", "\\'");
                    const attributeMap = new Map();
                    attributeMap.set('identifier', identifier);
                    attributeMap.set('name', name);
                    return gridjs.html(formatAllowedActions(cell, row, attributeMap));
                }
            }
        );

        this.columns = columns;
    }

    this.initGridActions = () => {
        delegateListItemActions('incidentsTable',
            (id, elem) => this.incidentService.editIncident('editIncidentDialog', id),
            (id, name, elem) => this.incidentService.deleteIncident(this.incidentGrid, id, name),
        )
    }

};

function columnLabel(field) {
    return field.indexColumnName || field.question;
}

function formatAsLink(label, href, shouldOpenInWindow = false, isDraft = false) {
    const nullSafeLabel = label === null || label === undefined ? '' : label;
    const nullSafeHref = href === null || href === undefined ? '#' : href;
    const target = shouldOpenInWindow ? ' target="_blank" rel="noopener noreferrer"' : '';
    const draftBadge = isDraft ? ' <span class="badge bg-warning">Kladde</span>' : '';
    return gridjs.html(`<a href="${nullSafeHref}" ${target}>${nullSafeLabel}</a>${draftBadge}`);
}
