package dk.digitalidentity.service.kle;

import dk.digitalidentity.dao.kle.KLEKeywordDao;
import dk.digitalidentity.model.entity.kle.KLEKeyword;
import dk.digitalidentity.model.entity.kle.KLESyncableService;
import dk.digitalidentity.model.entity.kle.Syncable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class KLEKeywordService implements KLESyncableService<KLEKeyword, String> {
	private final KLEKeywordDao kleKeywordDao;

	public List<KLEKeyword> getAll() {
		return kleKeywordDao.findAll();
	}

	public Set<KLEKeyword> findAllById(Set<String> hashedIds) {
		return kleKeywordDao.findByHashedIdIn(hashedIds);
	}

	public KLEKeyword save(KLEKeyword kleKeyword) {
		return kleKeywordDao.save(kleKeyword);
	}

	public void saveAll(Collection<KLEKeyword> keywords) {
		kleKeywordDao.saveAll(keywords);
	}

	public void delete(KLEKeyword kleKeyword) {
		kleKeywordDao.delete(kleKeyword);
	}

	public void deleteAll(Collection<KLEKeyword> keywords) {
		kleKeywordDao.deleteAll(keywords);
	}

	@Override
	public Set<String> findAllIds() {
		return kleKeywordDao.findAllIds();
	}

	@Override
	public void saveAllSyncables(Collection<KLEKeyword> entities) {
		kleKeywordDao.saveAll(entities);
	}

	public void deleteAllById(Collection<String> hashedIds) {
		kleKeywordDao.deleteAllById(hashedIds);
	}

	@Override
	public List<KLEKeyword> findAllById(Collection<String> strings) {
		return kleKeywordDao.findAllById(strings);
	}

}
