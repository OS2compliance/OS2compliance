package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLEMainGroupDao;
import dk.digitalidentity.model.entity.KLE.KLEMainGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class KLEMainGroupService {
	private final KLEMainGroupDao kleMainGroupDao;

	public KLEMainGroup save ( KLEMainGroup kleMainGroup) {
		return kleMainGroupDao.save(kleMainGroup);
	}

	public void deleteAll(){
		kleMainGroupDao.deleteAll();
	}
}
