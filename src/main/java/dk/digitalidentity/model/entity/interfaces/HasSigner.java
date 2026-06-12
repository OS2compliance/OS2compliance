package dk.digitalidentity.model.entity.interfaces;

/**
 * Marks a grid view entity that has a field with the uuid of the user assigned to sign/approve the entity
 */
public interface HasSigner {
	String getSignerUuid ();
}
