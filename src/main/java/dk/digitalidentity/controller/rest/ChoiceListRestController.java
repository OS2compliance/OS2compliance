package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.mapping.ChoiceListMapper;
import dk.digitalidentity.model.dto.ChoiceListDTO;
import dk.digitalidentity.model.dto.ChoiceValueDTO;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireSuperuser
@RequiredArgsConstructor
@RequestMapping(value = "rest/choice-lists", consumes = "application/json", produces = "application/json")
public class ChoiceListRestController {
    private final ChoiceListMapper mapper;
    private final ChoiceListDao choiceListDao;
    private final ChoiceValueDao choiceValueDao;

    @PostMapping("values")
    @Transactional
    public List<ChoiceValueDTO> createValues(@RequestBody @Valid final List<ChoiceValueDTO> choiceValueEO) {
        return choiceValueEO.stream()
                .peek(c -> {
                    final Optional<ChoiceValue> found = choiceValueDao.findByIdentifier(c.getIdentifier());
                    if (found.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Value already exist: " + c.getIdentifier());
                    }
                })
                .map(mapper::fromDTO)
                .map(choiceValueDao::save)
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @DeleteMapping("values/{identifier}")
    @Transactional
    public void deleteValue(@PathVariable("identifier") final String identifier) {
        final ChoiceValue choiceValue = choiceValueDao.findByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        choiceValueDao.delete(choiceValue);
    }

    @PostMapping
    @Transactional
    public ChoiceListDTO createList(@RequestBody @Valid final ChoiceListDTO choiceList) {
        final ChoiceList savedEntity = choiceListDao.save(mapper.fromDTO(choiceList));
        final Set<ChoiceValue> values = choiceList.getValueIdentifiers().stream()
                .map(identifier -> choiceValueDao.findByIdentifier(identifier)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced value not found")))
                .collect(Collectors.toSet());
        savedEntity.getValues().addAll(values);
        return mapper.toDTO(savedEntity);
    }

    @RequireUser
    @GetMapping("{identifier}")
    public ChoiceListDTO getList(@PathVariable("identifier") final String identifier) {
        final ChoiceList foundList = choiceListDao.findByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapper.toDTO(foundList);
    }


    @DeleteMapping("{identifier}")
    @Transactional
    public void deleteList(@PathVariable("identifier") final String identifier) {
        final ChoiceList foundList = choiceListDao.findByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        choiceListDao.delete(foundList);
    }


}
