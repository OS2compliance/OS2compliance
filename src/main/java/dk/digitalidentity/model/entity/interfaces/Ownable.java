package dk.digitalidentity.model.entity.interfaces;

import dk.digitalidentity.model.entity.User;

public interface Ownable {
	boolean isOwnedBy(User user);
}
