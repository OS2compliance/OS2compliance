package dk.digitalidentity.model.entity.kle;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface KLESyncableService<T extends Syncable<ID>, ID> {
	Set<ID> findAllIds();
	List<T> saveAllSyncables(Collection<T> entities);
	void deleteAllById(Collection<ID> ids);
	List<T> findAllById(Collection<ID> ids);
}
