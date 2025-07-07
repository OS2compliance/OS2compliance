package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEMainGroupDao;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
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
