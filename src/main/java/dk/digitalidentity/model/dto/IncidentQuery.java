package dk.digitalidentity.model.dto;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;

/**
 * Everything the incident log filters on, parsed out of the grid's query parameters.
 *
 * @param dateFilter    which date the from/to range applies to
 * @param from          start of the date range, inclusive; null means unbounded
 * @param to            end of the date range, inclusive; null means unbounded
 * @param search        free text matched across the title and every answer, or null
 * @param columnFilters filters on built-in columns, keyed by {@link dk.digitalidentity.model.entity.Incident} property
 * @param fieldFilters  filters on custom field columns, keyed by incident field id
 */
public record IncidentQuery(IncidentDateFilter dateFilter,
                            LocalDate from,
                            LocalDate to,
                            String search,
                            Map<String, String> columnFilters,
                            Map<Long, String> fieldFilters) {

    /**
     * Built-in incident properties the grid is allowed to filter and sort on. Anything else coming in
     * as a column filter is ignored rather than passed to the Criteria API, where an unknown property
     * would blow up the query.
     */
    public static final Set<String> BUILT_IN_COLUMNS = Set.of("name", "createdAt", "updatedAt");

    /**
     * Parameters the grid sends that are not column filters. {@code size} is the paging parameter the
     * endpoint used before it moved to CustomGridFunctions' {@code limit}; it stays listed so a
     * browser still holding the old saved grid state cannot smuggle it in as a column filter.
     */
    private static final Set<String> RESERVED_PARAMS =
        Set.of("page", "limit", "size", "order", "dir", "fileName", "search", "dateField", "fromDate", "toDate");

    /**
     * Builds a query from the raw request parameters sent by CustomGridFunctions.
     * Unknown parameters and unparsable dates are dropped, so a stale saved grid state degrades to a
     * wider result set instead of an error.
     */
    public static IncidentQuery of(final Map<String, String> params) {
        final Map<String, String> parameters = params != null ? params : Map.of();
        final Map<String, String> columnFilters = new HashMap<>();
        final Map<Long, String> fieldFilters = new HashMap<>();

        for (final Map.Entry<String, String> param : parameters.entrySet()) {
            final String key = param.getKey();
            final String value = param.getValue();
            if (RESERVED_PARAMS.contains(key) || StringUtils.isBlank(value)) {
                continue;
            }
            final Long fieldId = IncidentDateFilter.parseFieldId(key);
            if (fieldId != null) {
                fieldFilters.put(fieldId, value);
            } else if (BUILT_IN_COLUMNS.contains(key)) {
                columnFilters.put(key, value);
            }
        }

        return new IncidentQuery(IncidentDateFilter.parse(parameters.get("dateField")),
            parseDate(parameters.get("fromDate")),
            parseDate(parameters.get("toDate")),
            StringUtils.trimToNull(parameters.get("search")),
            columnFilters,
            fieldFilters);
    }

    private static LocalDate parseDate(final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, DK_DATE_FORMATTER);
        } catch (final DateTimeParseException e) {
            return null;
        }
    }
}
