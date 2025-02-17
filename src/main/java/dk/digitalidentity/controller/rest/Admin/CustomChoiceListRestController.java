package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.service.ChoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequireAdministrator
@RequiredArgsConstructor
@RequestMapping(value = "rest/choicelists/custom", consumes = "application/json", produces = "application/json")
public class CustomChoiceListRestController {

    private final ChoiceService choiceService;

    record CustomChoiceListDTO(Long id, String value) {
    }

    @Transactional
    @PutMapping("{listId}/update")
    public ResponseEntity<?> updateList(@PathVariable final Long listId, @RequestBody List<CustomChoiceListDTO> customChoiceListDTO) {

        ChoiceList choiceList = choiceService.findChoiceList(listId)
            .orElseThrow();

        List<ChoiceValue> updatedValues = customChoiceListDTO.stream().map(dto ->
                choiceService.updateOrCreate(ChoiceValue.builder()
                    .id(dto.id)
                    .identifier(choiceList.getIdentifier() + "-" + dto.value)
                    .caption(dto.value)
                    .build()))
            .toList();

        //For some reason it is not allowed to just replace list, so clear and add them instead
        List<ChoiceValue> existingList = choiceList.getValues();
        existingList.clear();
        existingList.addAll(updatedValues);

        choiceService.save(choiceList);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
