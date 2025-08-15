package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.User;

/**
 * Marks a grid view entity with a field for a single responsible user
 */
public interface HasSingleResponsibleUser {
	User getResponsibleUser();
}
