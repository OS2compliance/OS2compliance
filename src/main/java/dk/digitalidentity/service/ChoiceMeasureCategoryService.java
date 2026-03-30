package dk.digitalidentity.service;

import dk.digitalidentity.dao.ChoiceMeasureCategoryDao;
import dk.digitalidentity.model.entity.ChoiceMeasureCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChoiceMeasureCategoryService {
	private final ChoiceMeasureCategoryDao choiceMeasureCategoryDao;

	public List<ChoiceMeasureCategory> findAll() {
		return choiceMeasureCategoryDao.findAll();
	}

	public Optional<ChoiceMeasureCategory> findById(Long id) {
		return choiceMeasureCategoryDao.findById(id);
	}

	public void save(ChoiceMeasureCategory category) {
		choiceMeasureCategoryDao.save(category);
	}

	public void delete(ChoiceMeasureCategory category) {
		choiceMeasureCategoryDao.delete(category);
	}
}