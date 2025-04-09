package dk.digitalidentity.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FilterService {

    /**
     * Removes non-filter items like page,limit, order and dir from the filters, then ensures that each filter key exist on the given class
     * Any that does not exist on class is not included in the returned filter
     * @param filters
     * @param validationClass
     * @return a new map of validated filters
     */
    public Map<String, String> validateSearchFilters(Map<String, String> filters, Class<?> validationClass) {
        // Remove pagination/sorting parameters from the filter map
        filters.remove("page");
        filters.remove("limit");
        filters.remove("order");
        filters.remove("dir");

        Map<String, String> validatedFilters = new HashMap<>();
            List<String> classFields = Arrays.stream(validationClass.getDeclaredFields()).map(Field::getName).toList();
        for ( String filterKey : filters.keySet() ) {
            if (
                classFields.contains(filterKey)
                || classFields.contains( filterKey.split("\\.")[0])

            ) {
                validatedFilters.put(filterKey, filters.get(filterKey));
            }
        }
        return validatedFilters;
    }

    /**
     * Builds a Pageable from the provided input
     * @param page
     * @param limit
     * @param sortColumn
     * @param sortDirection
     * @return a Pageable
     */
    public Pageable buildPageable(int page, int limit, String sortColumn, String sortDirection) {
        //Set sorting
        Sort sort = null;
        if (StringUtils.isNotEmpty(sortColumn)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, sortColumn);
        } else {
            sort = Sort.unsorted();
        }
        return PageRequest.of(page, limit, sort);
    }
}
