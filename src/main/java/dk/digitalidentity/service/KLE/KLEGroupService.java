package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLEGroupDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class KLEGroupService {
	private final KLEGroupDao kleGroupdao;

}
