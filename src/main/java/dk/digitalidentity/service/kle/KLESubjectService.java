package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLESubjectDao;
import dk.digitalidentity.model.entity.kle.KLESubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Transactional
@RequiredArgsConstructor
@Service
public class KLESubjectService {
	private final KLESubjectDao kleSubjectDao;

	public List<KLESubject> getAll() {
		return kleSubjectDao.findAll();
	}

	public KLESubject save(KLESubject kleSubject) {
		return kleSubjectDao.save(kleSubject);
	}

	public void saveAll(Collection<KLESubject> kleSubjects) {
		kleSubjectDao.saveAll(kleSubjects);
	}
}
