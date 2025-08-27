package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLEKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;

public interface KLEKeywordDao  extends JpaRepository<KLEKeyword, String> {


	void deleteByHashedIdNotIn(Collection<String> hashedIds);

	Set<KLEKeyword> findByHashedIdIn(Collection<String> hashedIds);
}
