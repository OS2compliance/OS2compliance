package dk.digitalidentity.model.entity.kle;

public interface Syncable<ID> {
	ID getId();
	void markNew();
}
