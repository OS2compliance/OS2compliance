package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEMainGroupDao;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class KLEMainGroupService {
	private final KLEMainGroupDao kleMainGroupDao;

	public List<KLEMainGroup> getAll() {
		return kleMainGroupDao.findAll();
	}

	public Set<KLEMainGroup> getAllActive() {
		return kleMainGroupDao.findAllByDeletedFalse();
	}

	public Set<KLEMainGroup> getAllByMainGroupNumbers(Collection<String> mainGroupNumbers) {
		return kleMainGroupDao.findAllByDeletedFalseAndMainGroupNumberIn(mainGroupNumbers);
	}

	public KLEMainGroup save(KLEMainGroup kleMainGroup) {
		return kleMainGroupDao.save(kleMainGroup);
	}

	public List<KLEMainGroup> saveAll(List<KLEMainGroup> mainGroups) {
		return kleMainGroupDao.saveAll(mainGroups);
	}
}
