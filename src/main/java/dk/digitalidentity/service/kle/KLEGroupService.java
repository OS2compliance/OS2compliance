package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEGroupDao;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class KLEGroupService {
	private final KLEGroupDao kleGroupdao;

	public KLEGroup save(KLEGroup group) {
		return kleGroupdao.save(group);
	}
}
