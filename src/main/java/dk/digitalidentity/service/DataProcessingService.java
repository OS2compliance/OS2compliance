package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.model.dto.DataProcessingChoicesDTO;
import dk.digitalidentity.model.dto.DataProcessingDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static dk.digitalidentity.Constants.ASSOCIATED_INSPECTION_PROPERTY;

@Slf4j
@Service("dataProcessingService")
@RequiredArgsConstructor
public class DataProcessingService {
    private final ChoiceListDao choiceListDao;
    private final TaskService taskService;
    private final RelationService relationService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void update(final DataProcessing dataProcessing, final DataProcessingDTO body) {
        dataProcessing.setAccessWhoIdentifiers(body.getAccessWhoIdentifiers());
        dataProcessing.setAccessCountIdentifier(body.getAccessCountIdentifier());
        dataProcessing.setPersonCountIdentifier(body.getPersonCountIdentifier());
        dataProcessing.setStorageTimeIdentifier(body.getStorageTimeIdentifier());
        dataProcessing.setDeletionProcedure(body.getDeletionProcedure());
        dataProcessing.setDeletionProcedureLink(body.getDeletionProcedureLink());
        dataProcessing.setElaboration(body.getElaboration());
        dataProcessing.setTypesOfPersonalInformationFreetext(body.getTypesOfPersonalInformationFreetext());

        if (body.getPersonCategoriesRegistered() != null) {
            dataProcessing.getRegisteredCategories().clear();
            body.getPersonCategoriesRegistered().stream()
                    .filter(c -> !c.getPersonCategoriesRegisteredIdentifier().isEmpty())
                    .forEach(c -> dataProcessing.getRegisteredCategories()
                            .add(DataProcessingCategoriesRegistered.builder()
                                    .dataProcessing(dataProcessing)
                                    .personCategoriesRegisteredIdentifier(c.getPersonCategoriesRegisteredIdentifier())
                                    .personCategoriesInformationIdentifiers(c.getPersonCategoriesInformationIdentifiers())
                                    .informationPassedOn(c.getInformationPassedOn())
                                    .informationReceivers(c.getInformationReceivers())
                                    .build()));
        }

    }

    public DataProcessingChoicesDTO getChoices() {
        final ChoiceList accessWhoIdentifiers = choiceListDao.findByIdentifier("dp-access-who-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find accessWhoIdentifiers Choices"));

        final ChoiceList accessCountIdentifier = choiceListDao.findByIdentifier("dp-access-count-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find AccessCountIdentifier Choices"));

        final ChoiceList personCountIdentifier = choiceListDao.findByIdentifier("dp-count-processing-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCountIdentifier Choices"));

        final ChoiceList personCategoriesInformationIdentifiers1 = choiceListDao.findByIdentifier("dp-person-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers1 Choices"));

        final ChoiceList personCategoriesInformationIdentifiers2 = choiceListDao.findByIdentifier("dp-person-categories-sensitive-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers2 Choices"));

        final ChoiceList personCategoriesRegisteredIdentifiers = choiceListDao.findByIdentifier("dp-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesRegisteredIdentifiers Choices"));

        final ChoiceList storageTimeIdentifier = choiceListDao.findByIdentifier("dp-person-storage-duration-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find storageTimeIdentifier Choices"));

        final ChoiceList informationReceiversIdentifiers = choiceListDao.findByIdentifier("dp-receiver-list")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not find storageTimeIdentifier Choices"));

        return DataProcessingChoicesDTO.builder()
                .accessWhoIdentifiers(accessWhoIdentifiers)
                .accessCountIdentifier(accessCountIdentifier)
                .personCountIdentifier(personCountIdentifier)
                .personCategoriesInformationIdentifiers1(personCategoriesInformationIdentifiers1)
                .personCategoriesInformationIdentifiers2(personCategoriesInformationIdentifiers2)
                .personCategoriesRegisteredIdentifiers(personCategoriesRegisteredIdentifiers)
                .storageTimeIdentifier(storageTimeIdentifier)
                .informationReceiversIdentifiers(informationReceiversIdentifiers)
                .build();
    }


}
