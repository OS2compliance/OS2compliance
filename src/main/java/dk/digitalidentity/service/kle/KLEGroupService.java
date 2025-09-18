package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEGroupDao;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESyncableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Transactional
@RequiredArgsConstructor
@Service
public class KLEGroupService implements KLESyncableService<KLEGroup, String> {
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

	@Override
	public Set<String> findAllIds() {
		return kleGroupdao.findAllIds();
	}

	@Override
	public List<KLEGroup> saveAllSyncables(Collection<KLEGroup> entities) {
		return kleGroupdao.saveAll(entities);
	}

	@Override
	public void deleteAllById(Collection<String> strings) {
		kleGroupdao.softDeleteByGroupNumbers(strings);
	}

	@Override
	public List<KLEGroup> findAllById(Collection<String> strings) {
		return kleGroupdao.findAllById(strings);
	}
}
