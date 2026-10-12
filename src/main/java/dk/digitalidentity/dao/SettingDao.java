package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Setting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SettingDao extends JpaRepository<Setting, Long> {
	Optional<Setting> findBySettingKey(final String key);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Setting> findForUpdateBySettingKey(final String key);
    List<Setting> findByEditableTrue();
    List<Setting> findByAssociationAndEditableTrue(final String association);
	boolean existsBySettingKey(String key);
    List<Setting> findByAssociation(final String association);
}
