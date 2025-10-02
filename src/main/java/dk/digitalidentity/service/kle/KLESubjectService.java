package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLESubjectDao;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.digitalidentity.model.entity.kle.KLESyncableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Transactional
@RequiredArgsConstructor
@Service
public class KLESubjectService implements KLESyncableService<KLESubject, String> {
	private final KLESubjectDao kleSubjectDao;

	public List<KLESubject> findAllByIdIn(Collection<String> ids) {
		return kleSubjectDao.findAllById(ids);
	}

	public List<KLESubject> getAll() {
		return kleSubjectDao.findAll();
	}

	public KLESubject save(KLESubject kleSubject) {
		return kleSubjectDao.save(kleSubject);
	}

	public void saveAll(Collection<KLESubject> kleSubjects) {
		kleSubjectDao.saveAll(kleSubjects);
	}

	@Override
	public Set<String> findAllIds() {
		return kleSubjectDao.findAllIds();
	}

	@Override
	public List<KLESubject> saveAllSyncables(Collection<KLESubject> entities) {
		return kleSubjectDao.saveAll(entities);
	}

	@Override
	public void deleteAllById(Collection<String> strings) {
		kleSubjectDao.softDeleteBySubjectNumbers(strings);
	}

	@Override
	public List<KLESubject> findAllById(Collection<String> strings) {
		return findAllByIdIn(strings);
	}

	public Set<KLESubject> findAllBySubjectNumbers(Set<String> subjectIds) {
		return kleSubjectDao.findAllBySubjectNumberIn(subjectIds);
	}
}
