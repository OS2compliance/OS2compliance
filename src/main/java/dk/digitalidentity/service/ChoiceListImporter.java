package dk.digitalidentity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.dao.ChoiceMeasureCategoryDao;
import dk.digitalidentity.dao.ChoiceMeasuresDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.mapping.ChoiceDPIAMapper;
import dk.digitalidentity.mapping.ChoiceListMapper;
import dk.digitalidentity.mapping.ChoiceMeasuresMapper;
import dk.digitalidentity.model.dto.ChoiceDpiaDTO;
import dk.digitalidentity.model.dto.ChoiceListDTO;
import dk.digitalidentity.model.dto.ChoiceMeasureDTO;
import dk.digitalidentity.model.dto.ChoiceValueDTO;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceMeasure;
import dk.digitalidentity.model.entity.ChoiceMeasureCategory;
import dk.digitalidentity.model.entity.ChoiceValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChoiceListImporter {
    private final ObjectMapper objectMapper;
    private final ChoiceValueDao valueDao;
    private final ChoiceListDao listDao;
    private final ChoiceMeasuresDao choiceMeasuresDao;
    private final ChoiceDPIADao choiceDpiaDao;
    private final ChoiceListMapper mapper;
    private final ChoiceMeasuresMapper measuresMapper;
    private final ChoiceDPIAMapper dpiaMapper;
	private final ChoiceMeasureCategoryDao choiceMeasureCategoryDao;

    public ChoiceListImporter(final ObjectMapper objectMapper, final ChoiceValueDao valueDao, final ChoiceListDao listDao, final ChoiceListMapper mapper, final ChoiceMeasuresDao measureDao, final ChoiceMeasuresMapper measuresMapper, final ChoiceDPIADao choiceDpiaDao, final ChoiceDPIAMapper dpiaMapper, final ChoiceMeasureCategoryDao choiceMeasureCategoryDao) {
        this.objectMapper = objectMapper;
        this.valueDao = valueDao;
        this.listDao = listDao;
        this.mapper = mapper;
        this.choiceMeasuresDao = measureDao;
        this.measuresMapper = measuresMapper;
        this.choiceDpiaDao = choiceDpiaDao;
        this.dpiaMapper = dpiaMapper;
		this.choiceMeasureCategoryDao = choiceMeasureCategoryDao;
    }

    public void importValues(final String filename) throws IOException {
        final ChoiceValueDTO[] values = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), ChoiceValueDTO[].class);
        Arrays.stream(values)
                .filter(v -> !valueDao.existsByIdentifier(v.getIdentifier()))
                .map(mapper::fromDTO)
                .forEach(valueDao::save);
    }

    @Transactional
    public void updateValues(final String filename) throws IOException {
        final ChoiceValueDTO[] values = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), ChoiceValueDTO[].class);
        Arrays.stream(values)
            .forEach(v -> valueDao.findByIdentifier(v.getIdentifier())
                .ifPresent(entity -> {
                    entity.setCaption(v.getCaption());
                    entity.setDescription(v.getDescription());
                }));
    }

    public void importList(final String filename) throws IOException {
        log.info("Importing choice list {}", filename);
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final ChoiceListDTO list = objectMapper.readValue(jsonString, ChoiceListDTO.class);
        final ChoiceList entity = listDao.findByIdentifier(list.getIdentifier())
            .orElseGet(() -> mapper.fromDTO(list));
        final List<ChoiceValue> values = list.getValueIdentifiers().stream()
                .map(vid -> valueDao.findByIdentifier(vid).orElseThrow(() -> new RuntimeException("Value not found " + vid)))
                .collect(Collectors.toList());
        entity.setValues(values);
        listDao.save(entity);
    }

    public void importMeasuresList(final String filename) throws IOException {
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final List<ChoiceMeasureDTO> list = objectMapper.readValue(jsonString, new TypeReference<>() { });

		// Keep track of next sortOrder per category to avoid database lookups
		final Map<Long, Integer> nextSortOrderPerCategory = new HashMap<>();

        for (final ChoiceMeasureDTO choice : list) {
            if (!choiceMeasuresDao.existsByIdentifier(choice.getIdentifier())) {
                final ChoiceMeasure entity = measuresMapper.fromDTO(choice);

				// Find or create category
				final ChoiceMeasureCategory category = choiceMeasureCategoryDao
						.findByName(choice.getCategory())
						.orElseGet(() -> {
							final ChoiceMeasureCategory newCategory = new ChoiceMeasureCategory();
							newCategory.setName(choice.getCategory());
							newCategory.setSortOrder(getNextCategorySortOrder());
							newCategory.setDeleted(false);
							return choiceMeasureCategoryDao.save(newCategory);
						});
				entity.setCategory(category);

				// Get or initialize next sortOrder for this category
				Integer nextSortOrder = nextSortOrderPerCategory.computeIfAbsent(
						category.getId(),
						id -> getInitialMeasureSortOrder(category)
				);

				entity.setSortOrder(nextSortOrder);
				entity.setDeleted(false);

				// Increment for next measure in this category
				nextSortOrderPerCategory.put(category.getId(), nextSortOrder + 1);

                final List<ChoiceValue> values = choice.getValueIdentifiers().stream()
                        .map(vid -> valueDao.findByIdentifier(vid).orElseThrow(() -> new RuntimeException("Value not found " + vid)))
                        .collect(Collectors.toList());
                entity.setValues(values);
                choiceMeasuresDao.save(entity);
            } else {
                final ChoiceMeasure measure = choiceMeasuresDao.findByIdentifier(choice.getIdentifier()).orElseThrow();

				// Update category if it has changed
				if (!measure.getCategory().getName().equals(choice.getCategory())) {
					final ChoiceMeasureCategory category = choiceMeasureCategoryDao
							.findByName(choice.getCategory())
							.orElseGet(() -> {
								final ChoiceMeasureCategory newCategory = new ChoiceMeasureCategory();
								newCategory.setName(choice.getCategory());
								newCategory.setSortOrder(getNextCategorySortOrder());
								newCategory.setDeleted(false);
								return choiceMeasureCategoryDao.save(newCategory);
							});
					measure.setCategory(category);

					// Get or initialize next sortOrder for this category
					Integer nextSortOrder = nextSortOrderPerCategory.computeIfAbsent(
							category.getId(),
							id -> getInitialMeasureSortOrder(category)
					);

					measure.setSortOrder(nextSortOrder);

					// Increment for next measure in this category
					nextSortOrderPerCategory.put(category.getId(), nextSortOrder + 1);
				}

				final List<String> identifiersToRemove = measure.getValues().stream()
						.map(ChoiceValue::getIdentifier)
						.collect(Collectors.toCollection(ArrayList::new));
				final List<String> wantedIdentifiers = choice.getValueIdentifiers();
                wantedIdentifiers.forEach(vid -> {
                        if (!identifiersToRemove.contains(vid)) {
							measure.getValues().add(valueDao.findByIdentifier(vid)
									.orElseThrow(() -> new RuntimeException("Value not found " + vid)));
                        }
                        identifiersToRemove.remove(vid);
                    });
                measure.getValues().removeIf(v -> identifiersToRemove.contains(v.getIdentifier()));
            }
        }
    }

	private Integer getInitialMeasureSortOrder(final ChoiceMeasureCategory category) {
		return category.getMeasures().stream()
				.filter(m -> !m.getDeleted())
				.map(ChoiceMeasure::getSortOrder)
				.max(Integer::compareTo)
				.map(max -> max + 1)
				.orElse(1);
	}

	private Integer getNextCategorySortOrder() {
		return choiceMeasureCategoryDao.findAll().stream()
				.map(ChoiceMeasureCategory::getSortOrder)
				.max(Integer::compareTo)
				.map(max -> max + 1)
				.orElse(1);
	}

    public void importDPIAList(final String filename) throws IOException {
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final List<ChoiceDpiaDTO> list = objectMapper.readValue(jsonString, new TypeReference<List<ChoiceDpiaDTO>>() { });
        for (final ChoiceDpiaDTO choice : list) {
            if (!choiceDpiaDao.existsByIdentifier(choice.getIdentifier())) {
                final ChoiceDPIA entity = dpiaMapper.fromDTO(choice);
                final Set<ChoiceValue> values = choice.getValueIdentifiers().stream()
                        .map(vid -> valueDao.findByIdentifier(vid).orElseThrow(() -> new RuntimeException("Value not found " + vid)))
                        .collect(Collectors.toSet());
                entity.setValues(values);
                choiceDpiaDao.save(entity);
            }
        }
    }

}
