package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SecurityUserService {

	private final UserService userService;

	public User getCurrentUserOrThrow() {
		final String userUuid = SecurityUtil.getLoggedInUserUuid();
		if (userUuid == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No logged in user");
		}

		return userService.findByUuid(userUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
	}

	public boolean canReadAll() {
		return SecurityUtil.isOperationAllowed(Roles.READ_ALL);
	}
}
