package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLEMainGroupDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KLEMainGroupService {
	private final KLEMainGroupDao kleMainGroupDao;

}
