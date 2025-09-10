package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEGroupDao;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Transactional
@RequiredArgsConstructor
@Service
public class KLEGroupService {
	private final KLEGroupDao kleGroupdao;

	public List<KLEGroup> getAll() {
		return kleGroupdao.findAll();
	}

	public Set<KLEGroup> getAllForMainGroups(Collection<KLEMainGroup> mainGroups) {
		return kleGroupdao.findAllByMainGroupIn(mainGroups);
	}

	public Set<KLEGroup> getAllByGroupNumbers(Collection<String> groupNumbers) {
		return kleGroupdao.findAllByDeletedFalseAndGroupNumberIn(groupNumbers);
	}

	public KLEGroup save(KLEGroup group) {
		return kleGroupdao.save(group);
	}

	public void saveAll(Collection<KLEGroup> groups) {
		kleGroupdao.saveAll(groups);
	}

	public Set<KLEGroup> findAllByMainGroupNumbers(Collection<String> mainGroupNumbers) {
		return kleGroupdao.findByMainGroup_MainGroupNumberIn(mainGroupNumbers);
	}
}
