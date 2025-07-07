package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLELegalReferenceDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KLELegalReferenceService {
	private final KLELegalReferenceDao kleLegalReferenceDao;

}
