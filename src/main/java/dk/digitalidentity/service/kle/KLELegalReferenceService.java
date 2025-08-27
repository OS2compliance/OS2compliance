package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLELegalReferenceDao;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class KLELegalReferenceService {
	private final KLELegalReferenceDao kleLegalReferenceDao;

	public List<KLELegalReference> getAll() {
		return kleLegalReferenceDao.findAll();
	}


	public Set<KLELegalReference> getAllWithAccessionNumberIn(Collection<String> accessionNumber) {
		return kleLegalReferenceDao.findByDeletedFalseAndAccessionNumberIn(accessionNumber);
	}

	public KLELegalReference save(KLELegalReference kleLegalReference) {
		return kleLegalReferenceDao.save(kleLegalReference);
	}

	public void saveAll(Collection<KLELegalReference> kleLegalReferences) {
		kleLegalReferenceDao.saveAll(kleLegalReferences);
	}
}
