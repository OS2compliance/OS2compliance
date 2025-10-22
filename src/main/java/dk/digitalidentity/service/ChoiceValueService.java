package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.entity.ChoiceValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChoiceValueService {
    private final ChoiceValueDao choiceValueDao;


	public Optional<ChoiceValue> findById(long id) {
		return choiceValueDao.findById(id);
	}

	public Optional<ChoiceValue> findByIdentifier(String identifier) {
		return choiceValueDao.findByIdentifier(identifier);
	}
}
