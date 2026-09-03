package dk.digitalidentity.model.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class IncidentQueryTest {

    @Test
    public void splitsColumnFiltersFromFieldFilters() {
        IncidentQuery query = IncidentQuery.of(Map.of(
                "name", "phishing",
                "field_42", "Økonomi"));

        assertThat(query.columnFilters()).containsExactly(entry("name", "phishing"));
        assertThat(query.fieldFilters()).containsExactly(entry(42L, "Økonomi"));
    }

    @Test
    public void ignoresPagingAndSortingParameters() {
        IncidentQuery query = IncidentQuery.of(Map.of(
                "page", "2",
                "limit", "50",
                "order", "createdAt",
                "dir", "desc",
                "fileName", "export.xlsx"));

        assertThat(query.columnFilters()).isEmpty();
        assertThat(query.fieldFilters()).isEmpty();
    }

    @Test
    public void ignoresUnknownColumns() {
        // The grid stores its filters in localStorage, so a column that has since been renamed or
        // removed can still turn up here. Passing it on would blow up the Criteria query.
        IncidentQuery query = IncidentQuery.of(Map.of("noSuchColumn", "x"));

        assertThat(query.columnFilters()).isEmpty();
    }

    @Test
    public void ignoresBlankFilterValues() {
        IncidentQuery query = IncidentQuery.of(Map.of("name", "  ", "field_42", ""));

        assertThat(query.columnFilters()).isEmpty();
        assertThat(query.fieldFilters()).isEmpty();
    }

    @Test
    public void parsesDanishDates() {
        IncidentQuery query = IncidentQuery.of(Map.of(
                "fromDate", "01/03-2026",
                "toDate", "31/03-2026"));

        assertThat(query.from()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(query.to()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    public void treatsUnparsableDateAsNoBound() {
        IncidentQuery query = IncidentQuery.of(Map.of("fromDate", "i går"));

        assertThat(query.from()).isNull();
    }

    @Test
    public void defaultsToCreatedWhenNoDateFieldIsGiven() {
        IncidentQuery query = IncidentQuery.of(Map.of());

        assertThat(query.dateFilter()).isEqualTo(IncidentDateFilter.DEFAULT);
        assertThat(query.dateFilter().target()).isEqualTo(IncidentDateFilter.Target.CREATED);
    }

    @Test
    public void readsTheChosenDateField() {
        IncidentQuery query = IncidentQuery.of(Map.of("dateField", "field_42"));

        assertThat(query.dateFilter().target()).isEqualTo(IncidentDateFilter.Target.FIELD);
        assertThat(query.dateFilter().fieldId()).isEqualTo(42L);
    }

    @Test
    public void chosenDateFieldIsNotAlsoAColumnFilter() {
        IncidentQuery query = IncidentQuery.of(Map.of("dateField", "field_42"));

        assertThat(query.fieldFilters()).isEmpty();
    }

    @Test
    public void fallsBackToCreatedOnAGarbledDateField() {
        assertThat(IncidentDateFilter.parse("field_").target()).isEqualTo(IncidentDateFilter.Target.CREATED);
        assertThat(IncidentDateFilter.parse("field_abc").target()).isEqualTo(IncidentDateFilter.Target.CREATED);
        assertThat(IncidentDateFilter.parse("noget andet").target()).isEqualTo(IncidentDateFilter.Target.CREATED);
    }

    @Test
    public void survivesAFieldIdTooLargeForALong() {
        // Straight off the query string, so it has to be treated as "not a field id" rather than
        // thrown at Long.valueOf.
        assertThat(IncidentDateFilter.parseFieldId("field_99999999999999999999")).isNull();
        assertThat(IncidentDateFilter.parse("field_99999999999999999999").target())
                .isEqualTo(IncidentDateFilter.Target.CREATED);
        assertThat(IncidentQuery.of(Map.of("field_99999999999999999999", "x")).fieldFilters()).isEmpty();
    }

    @Test
    public void readsUpdatedCaseInsensitively() {
        assertThat(IncidentDateFilter.parse("updated").target()).isEqualTo(IncidentDateFilter.Target.UPDATED);
    }

    @Test
    public void parsesCommaSeparatedAssetIds() {
        IncidentQuery query = IncidentQuery.of(Map.of("assetIds", "12, 34,56"));

        assertThat(query.assetIds()).containsExactly(12L, 34L, 56L);
    }

    @Test
    public void defaultsToNoAssetRestriction() {
        IncidentQuery query = IncidentQuery.of(Map.of());

        assertThat(query.assetIds()).isEmpty();
    }

    @Test
    public void assetIdsAreNotAlsoAColumnFilter() {
        IncidentQuery query = IncidentQuery.of(Map.of("assetIds", "12"));

        assertThat(query.columnFilters()).isEmpty();
    }
}
