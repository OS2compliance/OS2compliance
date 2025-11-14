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

	public ChoiceValue findByIdentifier(final String identifier) {
		return choiceValueDao.findByIdentifier(identifier).orElse(null);
	}

	public Optional<ChoiceValue> findOptionalByIdentifier(final String identifier) {
		return choiceValueDao.findByIdentifier(identifier);
	}

	public ChoiceValue save(ChoiceValue choiceValue) {
		return choiceValueDao.save(choiceValue);
	}

	public void delete(ChoiceValue choiceValue) {
		choiceValueDao.delete(choiceValue);
	}
}
