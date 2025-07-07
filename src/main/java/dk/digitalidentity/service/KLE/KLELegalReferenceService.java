package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLELegalReferenceDao;
import dk.digitalidentity.model.entity.KLE.KLELegalReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KLELegalReferenceService {
	private final KLELegalReferenceDao kleLegalReferenceDao;

	public KLELegalReference save(KLELegalReference kleLegalReference) {
		return kleLegalReferenceDao.save(kleLegalReference);
	}
}
