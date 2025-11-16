package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLEKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;

public interface KLEKeywordDao  extends JpaRepository<KLEKeyword, String> {


	void deleteByHashedIdNotIn(Collection<String> hashedIds);

	Set<KLEKeyword> findByHashedIdIn(Collection<String> hashedIds);

	@Query("SELECT k.hashedId FROM KLEKeyword k")
	Set<String> findAllIds();
}
