package dk.digitalidentity.model.entity;

/**
 * Marks a grid view entity with a field for a single responsible user
 */
public interface HasSingleResponsibleUser {
	User getResponsibleUser();
}
