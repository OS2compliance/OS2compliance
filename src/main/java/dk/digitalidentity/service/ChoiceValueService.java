package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.model.entity.ChoiceValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChoiceValueService {

	private final ChoiceValueDao choiceValueDao;

	private final List<String> protectedValues = List.of("register-status-not-started-123456", "register-status-in-progress-123456", "register-status-ready-123456");

	public Optional<ChoiceValue> findById(long id) {
		return choiceValueDao.findById(id);
	}

	public ChoiceValue findByIdentifier(final String identifier) {
		return choiceValueDao.findByIdentifier(identifier).orElse(null);
	}

	public void save(ChoiceValue choiceValue) {
		choiceValueDao.save(choiceValue);
	}

	public void delete(ChoiceValue choiceValue) {
		choiceValueDao.delete(choiceValue);
	}

	public boolean isProtected(ChoiceValue choiceValue) {
		return protectedValues.contains(choiceValue.getIdentifier());
	}
}
