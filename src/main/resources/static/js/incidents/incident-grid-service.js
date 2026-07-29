import ColumnOptions from "../grid-js-extension/column-options.js";
import IncidentService from "./incident-service.js";
import { initSaveAsExcelButton } from "/js/excel-export/excel-export-init.js";

// Custom incident fields are addressed by id, never by their column heading: the heading is free text
// an administrator can rename at any time, which would silently break every saved filter.
const FIELD_PREFIX = 'field_';

const SEARCH_DEBOUNCE_MS = 1000;

export default function IncidentGridService () {
    this.incidentService = new IncidentService();

    this.customFields = [];
    this.dateFields = [];
    this.customGridFunctions = null;

    this.init = async () => {
        this.customFields = await this.incidentService.fetchColumns() || [];
        this.dateFields = await this.incidentService.fetchDateFields() || [];
        // The grid goes up first: CustomGridFunctions restores the persisted filters and fetches with
        // them, and the toolbar controls below read their values back out of that same state.
        this.initGrid();
        this.initDateFieldSelect();
        this.initDatePickers();
        this.initSearch();
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
            dateField: this.filterValue('dateField') || 'CREATED',
            from: this.filterValue('fromDate'),
            to: this.filterValue('toDate')
        }).toString();
    }

    /**
     * The toolbar filters are kept in the grid's own search state, the same place the column filters
     * live. That is what carries them to the server and into the Excel export, and CustomGridFunctions
     * persists it, so there is no second copy to keep in step.
     */
    this.filterValue = (key) => this.customGridFunctions.state.searchValues[key] || '';

    this.setFilter = (key, value) => {
        this.customGridFunctions.updateColumnValue(key, value || '');
        // Narrowing the result set while standing on page 4 would otherwise ask the server for a
        // page that no longer exists, and the grid would come back empty.
        this.customGridFunctions.state.page = 0;
        this.customGridFunctions.saveState();
        this.customGridFunctions.onSearch();
    }

    this.initDatePickers = () => {
        this.initDatePicker('#filterFromBtn', '#filterFrom', 'fromDate');
        this.initDatePicker('#filterToBtn', '#filterTo', 'toDate');
    }

    this.initDatePicker = (buttonSelector, inputSelector, filterKey) => {
        const picker = initDatepicker(buttonSelector, inputSelector);
        const saved = parseDkDate(this.filterValue(filterKey));
        if (saved) {
            picker.setFullDate(saved);
        }
        picker.onSelect((date, formatedDate) => this.setFilter(filterKey, formatedDate));
        // "Ryd" empties the input without firing onSelect, so without this the box goes blank while
        // the grid and the reports keep filtering on the old date.
        picker.onClear(() => this.setFilter(filterKey, ''));
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

        // A field can be removed or made optional after the choice was saved. The grid has already
        // fetched by now, but the server falls back to the creation date for a field it cannot use, so
        // the rows on screen are the right ones — only the saved value needs correcting.
        const saved = this.filterValue('dateField') || 'CREATED';
        if (Array.from(select.options).some(option => option.value === saved)) {
            select.value = saved;
        } else {
            this.customGridFunctions.updateColumnValue('dateField', 'CREATED');
            this.customGridFunctions.saveState();
        }

        select.addEventListener("change", (event) => this.setFilter('dateField', event.target.value));
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
        input.value = this.filterValue('search');

        let debounce;
        input.addEventListener("input", (event) => {
            clearTimeout(debounce);
            const value = event.target.value;
            debounce = setTimeout(() => this.setFilter('search', value), SEARCH_DEBOUNCE_MS);
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

        this.initGridActions()
        initSaveAsExcelButton(this.customGridFunctions, 'incident', 'incidents', 'Hændelseslog');
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

/**
 * Reads back a date the grid state holds in the format the datepicker writes it, so the picker can be
 * restored from the same value the server filters on.
 */
function parseDkDate(value) {
    const parts = /^(\d{2})\/(\d{2})-(\d{4})$/.exec(value);
    return parts ? new Date(Number(parts[3]), Number(parts[2]) - 1, Number(parts[1])) : null;
}

function formatAsLink(label, href, shouldOpenInWindow = false, isDraft = false) {
    const nullSafeLabel = label === null || label === undefined ? '' : label;
    const nullSafeHref = href === null || href === undefined ? '#' : href;
    const target = shouldOpenInWindow ? ' target="_blank" rel="noopener noreferrer"' : '';
    const draftBadge = isDraft ? ' <span class="badge bg-warning">Kladde</span>' : '';
    return gridjs.html(`<a href="${nullSafeHref}" ${target}>${nullSafeLabel}</a>${draftBadge}`);
}
