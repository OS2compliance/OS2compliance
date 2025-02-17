package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceListDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChoiceService {
    private final ChoiceValueDao choiceValueDao;
    private final ChoiceListDao choiceListDao;

    public Optional<ChoiceList> findChoiceList(long id) {
        return choiceListDao.findById(id);
    }

    public ChoiceService(final ChoiceValueDao choiceValueDao, final ChoiceListDao choiceListDao) {
        this.choiceValueDao = choiceValueDao;
        this.choiceListDao = choiceListDao;
    }

    public List<ChoiceValue> getValues(final Set<String> identifiers) {
        return choiceValueDao.findByIdentifierIn(identifiers);
    }

    public Optional<ChoiceValue> getValue(final String identifier) {
        return choiceValueDao.findByIdentifier(identifier);
    }

    public String getCaption(final String identifier, final String defaultValue) {
        return choiceValueDao.findByIdentifier(identifier).map(ChoiceValue::getCaption).orElse(defaultValue);
    }

    public Optional<ChoiceList> findChoiceList(final String identifier) {
        return choiceListDao.findByIdentifier(identifier);
    }

    public List<ChoiceValue> getChoicesOrderedByIdentifier(final ChoiceList choiceList) {
        return choiceList.getValues().stream()
            .sorted(Comparator.comparing(ChoiceValue::getIdentifier))
            .collect(Collectors.toList());
    }

    public List<ChoiceList> getAllCustomizableChoiceLists() {
        return choiceListDao.findByCustomizableTrue();
    }

    public ChoiceList save(ChoiceList choiceList) {
        return choiceListDao.save(choiceList);
    }

    public ChoiceValue updateOrCreate(ChoiceValue choiceValue) {
        if (choiceValue.getId() != null) {
            ChoiceValue foundValue = choiceValueDao.findById(choiceValue.getId())
                .orElseThrow();
            foundValue.setCaption(choiceValue.getCaption());
            foundValue.setDescription(choiceValue.getDescription());
            foundValue.setIdentifier(choiceValue.getIdentifier());
            foundValue.setLimitLower(choiceValue.getLimitLower());
            foundValue.setLimitUpper(choiceValue.getLimitUpper());
            foundValue.setLists(choiceValue.getLists());
            foundValue.setChildListIdentifier(choiceValue.getChildListIdentifier());
            return choiceValueDao.save(foundValue);
        } else {
            ChoiceValue createdValue = ChoiceValue.builder()
                .identifier(choiceValue.getIdentifier())
                .caption(choiceValue.getCaption())
                .limitLower(choiceValue.getLimitLower())
                .limitUpper(choiceValue.getLimitUpper())
                .lists(choiceValue.getLists())
                .childListIdentifier(choiceValue.getChildListIdentifier())
                .build();
            return choiceValueDao.save(createdValue);
        }
    }

}
