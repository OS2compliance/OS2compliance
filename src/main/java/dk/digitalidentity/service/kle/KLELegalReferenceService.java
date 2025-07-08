package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLELegalReferenceDao;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@RequiredArgsConstructor
@Service
public class KLELegalReferenceService {
	private final KLELegalReferenceDao kleLegalReferenceDao;

	public KLELegalReference save(KLELegalReference kleLegalReference) {
		return kleLegalReferenceDao.save(kleLegalReference);
	}

	public void softDeleteAllNotMatching(Collection<String> accessionNumbers) {
		kleLegalReferenceDao.softDeleteByAccessionNumbers(accessionNumbers);
	}
}
