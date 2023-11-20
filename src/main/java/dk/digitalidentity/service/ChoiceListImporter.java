package dk.digitalidentity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.ChoiceListDao;
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
import dk.digitalidentity.model.entity.ChoiceValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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

    public ChoiceListImporter(final ObjectMapper objectMapper, final ChoiceValueDao valueDao, final ChoiceListDao listDao, final ChoiceListMapper mapper, final ChoiceMeasuresDao measureDao, final ChoiceMeasuresMapper measuresMapper, final ChoiceDPIADao choiceDpiaDao, final ChoiceDPIAMapper dpiaMapper) {
        this.objectMapper = objectMapper;
        this.valueDao = valueDao;
        this.listDao = listDao;
        this.mapper = mapper;
        this.choiceMeasuresDao = measureDao;
        this.measuresMapper = measuresMapper;
        this.choiceDpiaDao = choiceDpiaDao;
        this.dpiaMapper = dpiaMapper;
    }

    public void importValues(final String filename) throws IOException {
        final ChoiceValueDTO[] values = objectMapper.readValue(new ClassPathResource(filename).getInputStream(), ChoiceValueDTO[].class);
        Arrays.stream(values)
                .filter(v -> !valueDao.existsByIdentifier(v.getIdentifier()))
                .map(mapper::fromDTO)
                .forEach(valueDao::save);

    }

    public void importList(final String filename) throws IOException {
        log.info("Importing choice list " + filename);
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final ChoiceListDTO list = objectMapper.readValue(jsonString, ChoiceListDTO.class);
        if (!listDao.existsByIdentifier(list.getIdentifier())) {
            final ChoiceList entity = mapper.fromDTO(list);
            final List<ChoiceValue> values = list.getValueIdentifiers().stream()
                    .map(vid -> valueDao.findByIdentifier(vid).orElseThrow(() -> new RuntimeException("Value not found " + vid)))
                    .collect(Collectors.toList());
            entity.setValues(values);
            listDao.save(entity);
        }
    }


    public void importMeasuresList(final String filename) throws IOException {
        final InputStream inputStream = new ClassPathResource(filename).getInputStream();
        final String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        final List<ChoiceMeasureDTO> list = objectMapper.readValue(jsonString, new TypeReference<>() { });
        for (final ChoiceMeasureDTO choice : list) {
            if (!choiceMeasuresDao.existsByIdentifier(choice.getIdentifier())) {
                final ChoiceMeasure entity = measuresMapper.fromDTO(choice);
                final List<ChoiceValue> values = choice.getValueIdentifiers().stream()
                        .map(vid -> valueDao.findByIdentifier(vid).orElseThrow(() -> new RuntimeException("Value not found " + vid)))
                        .collect(Collectors.toList());
                entity.setValues(values);
                choiceMeasuresDao.save(entity);
            }
        }
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
