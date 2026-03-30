package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceMeasuresDao;
import dk.digitalidentity.model.entity.ChoiceMeasure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChoiceMeasuresService {
	private final ChoiceMeasuresDao choiceMeasuresDao;

	public List<ChoiceMeasure> findAll() {
		return choiceMeasuresDao.findAll();
	}

	public Optional<ChoiceMeasure> findById(Long id) {
		return choiceMeasuresDao.findById(id);
	}

	public void save(ChoiceMeasure measure) {
		choiceMeasuresDao.save(measure);
	}

	public void delete(ChoiceMeasure measure) {
		choiceMeasuresDao.delete(measure);
	}
}