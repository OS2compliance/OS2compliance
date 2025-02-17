package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequireAdministrator
@RequiredArgsConstructor
@RequestMapping(value = "rest/choicelists/custom", consumes = "application/json", produces = "application/json")
public class CustomChoiceListRestController {

    private final ChoiceService choiceService;
    private final AssetService assetService;

    record CustomChoiceListDTO(Long id, String value) {
    }

    @Transactional
    @PutMapping("{listId}/update")
    public ResponseEntity<?> updateList(@PathVariable final Long listId, @RequestBody List<CustomChoiceListDTO> customChoiceListDTOs) {

        ChoiceList choiceList = choiceService.findChoiceList(listId)
            .orElseThrow();
        List<Long> existingIds = choiceList.getValues().stream().map(ChoiceValue::getId).toList();
        List<Long> updatedIds = customChoiceListDTOs.stream().map(dto -> dto.id).toList();

        List<ChoiceValue> finaLChoiceValues = new ArrayList<>();
        //find values that needs to be deleted:
        List<Long> markedForRemoval = existingIds.stream()
            .filter(existingId -> !updatedIds.contains(existingId))
//            .filter() //TODO - check if any assets use this value
            .toList();
        for(Long id : markedForRemoval) {
            choiceService.delete(id);
        }


        //find values that needs to be updated:
        List<CustomChoiceListDTO> markedForUpdate = customChoiceListDTOs.stream()
            .filter(dto -> existingIds.contains(dto.id)).toList();

        for (CustomChoiceListDTO choiceValue : markedForUpdate) {
            ChoiceValue updatedChoiceValue = choiceService.update(choiceValue.id, choiceValue.value());
                finaLChoiceValues.add(updatedChoiceValue);
        }

        //find values that needs to be created:
        List<CustomChoiceListDTO> markedForCreation = customChoiceListDTOs.stream()
            .filter(dto -> !existingIds.contains(dto.id)).toList();

        List<ChoiceValue> createdValues = markedForCreation.stream()
            .map(dto -> {
                return ChoiceValue.builder()
                    .identifier(choiceList.getIdentifier() + "-" + dto.value.toLowerCase().replace(" ", "-")+"-"+ RandomStringUtils.randomAlphanumeric(6))
                    .caption(dto.value)
                    .build();
            }).toList();

        for (ChoiceValue choiceValue : createdValues) {
            ChoiceValue savedValue = choiceService.save(choiceValue);
            finaLChoiceValues.add(savedValue);
        }

        choiceList.setValues(finaLChoiceValues);
        choiceService.save(choiceList);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
