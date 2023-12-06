package dk.digitalidentity.model.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static dk.digitalidentity.model.api.Examples.PAGE_CURRENT_ITEMS_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.PAGE_TOTAL_COUNT_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.PAGE_TOTAL_PAGES_EXAMPLE;
import static dk.digitalidentity.model.api.Examples.PAGE_TOTAL_PAGE_NUMBER_EXAMPLE;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class PageEO<T> {
    @Schema(description = "Total number of pages available", example = PAGE_TOTAL_PAGES_EXAMPLE)
    private int totalPages;
    @Schema(description = "The current page number", example = PAGE_TOTAL_PAGE_NUMBER_EXAMPLE)
    private int page;
    @Schema(description = "Total number of items available", example = PAGE_TOTAL_COUNT_EXAMPLE)
    private long totalCount;
    @Schema(description = "Number of items in the current page", example = PAGE_CURRENT_ITEMS_EXAMPLE)
    private int count;
    @Schema(description = "The content of the current page")
    private List<T> content;
}
