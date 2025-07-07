package dk.digitalidentity.service.KLE;

import dk.digitalidentity.dao.KLE.KLEGroupDao;
import dk.digitalidentity.model.entity.KLE.KLEGroup;
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
