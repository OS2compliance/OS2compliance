package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLESubjectDao;
import dk.digitalidentity.model.entity.KLE.KLESubject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class KLESubjectService {
	private final KLESubjectDao kleSubjectDao;

	public KLESubject save(KLESubject kleSubject) {
		return kleSubjectDao.save(kleSubject);
	}
}
