package dk.digitalidentity.security;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class RolePostProcessor implements SamlLoginPostProcessor {
	public static final String ATTRIBUTE_USERID = "ATTRIBUTE_USERID";
	public static final String ATTRIBUTE_USER_UUID = "ATTRIBUTE_USER_UUID";
	public static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
	private final OS2complianceConfiguration configuration;
	private final UserService userService;

	@Override
	public void process(final TokenUser tokenUser) {
		final String principal = tokenUser.getUsername();
		// C=DK,O=29189714,CN=Kaspar Bach Pedersen,Serial=8c3e3251-a9b1-45bd-af57-f5c202cca97b
		log.info("Principal: " + principal);
		final User user = extractUser(principal);

		tokenUser.getAttributes().put(ATTRIBUTE_USER_UUID, user.getUuid());
		tokenUser.getAttributes().put(ATTRIBUTE_USERID, user.getUserId());
		tokenUser.getAttributes().put(ATTRIBUTE_NAME, user.getName());
		tokenUser.setUsername(user.getUuid());

		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		// TODO
//		for (final var a : tokenUser.getAuthorities()) {
//			if (configuration.getAuthorityAdministrator().equals(a.getAuthority())) {
//				// Admins has access to everything
//				authorities.addAll(grantAdminAuthorities());
//			}
//			else if (configuration.getAuthoritySuperuser().equals(a.getAuthority())) {
//				// Superusers can: create, update and delete limited parts of the system
//				authorities.addAll(grantSuperUserAuthorities());
//			}
//			else if (configuration.getAuthorityUser().equals(a.getAuthority())) {
//				// Users can read limited parts of the app.
//				// They can Delete and Update ONLY if they are responsible for the entity
//				authorities.addAll(grantUserAuthorities());
//			}
//			else if (configuration.getAuthorityLimitedUser().equals(a.getAuthority())) {
//				// Limited users can read very limited parts of the app.
//				// They can Create, Delete and Update ONLY if they are responsible for the entity
//				authorities.addAll(grantLimitedUserAuthorities());
//			} else if (configuration.getAuthorityReadOnlyUser().equals(a.getAuthority())) {
//				// Read only users can see everything in a subsection of the system, but has no rights to modify
//				authorities.addAll(grantReadOnlyUserAuthorities());
//			}
//		}
		authorities.add(new SamlGrantedAuthority(Roles.AUTHENTICATED));
		// Add roles coming directly from the database.
		user.getRoles().forEach(r -> authorities.add(new SamlGrantedAuthority(r)));
		tokenUser.setAuthorities(authorities);
	}

	private User extractUser(final String principal) {
		final Optional<String> uuidOptional = NameIdParser.parseNameId(principal);
		return uuidOptional.map(uuid -> userService.findByUuid(uuid)
						.orElseThrow(() -> new UsernameNotFoundException("Brugeren for principal " + principal + " blev ikke fundet")))
				.orElseThrow(() -> new UsernameNotFoundException("Principal '" + principal + "' kunne ikke parses"));
	}

	private Set<SamlGrantedAuthority> grantAdminAuthorities() {
		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SamlGrantedAuthority(Roles.ADMINISTRATOR));

		authorities.add(new SamlGrantedAuthority(Roles.CREATE_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.READ_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.UPDATE_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.DELETE_ALL));

		authorities.add(new SamlGrantedAuthority(Roles.SECTION_CONFIGURATION));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ADMIN));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ASSET));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_STANDARD));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REGISTER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_SUPPLIER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_RISK_ASSESSMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_DOCUMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_TASK));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REPORT));
		return authorities;
	}

	private Set<SamlGrantedAuthority> grantSuperUserAuthorities() {
		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SamlGrantedAuthority(Roles.SUPER_USER));

		authorities.add(new SamlGrantedAuthority(Roles.CREATE_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.READ_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.UPDATE_ALL));
		authorities.add(new SamlGrantedAuthority(Roles.DELETE_ALL));

		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ASSET));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_STANDARD));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REGISTER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_SUPPLIER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_RISK_ASSESSMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_DOCUMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_TASK));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REPORT));
		return authorities;
	}

	private Set<SamlGrantedAuthority> grantUserAuthorities() {
		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SamlGrantedAuthority(Roles.USER));

		authorities.add(new SamlGrantedAuthority(Roles.CREATE_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.READ_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.UPDATE_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.DELETE_OWNER_ONLY));

		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ASSET));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_STANDARD));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REGISTER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_SUPPLIER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_RISK_ASSESSMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_DOCUMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_TASK));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REPORT));
		return authorities;
	}

	private Set<SamlGrantedAuthority> grantLimitedUserAuthorities() {
		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SamlGrantedAuthority(Roles.LIMITED_USER));

		authorities.add(new SamlGrantedAuthority(Roles.CREATE_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.READ_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.UPDATE_OWNER_ONLY));
		authorities.add(new SamlGrantedAuthority(Roles.DELETE_OWNER_ONLY));

		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ASSET));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_TASK));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REGISTER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REPORT));
		return authorities;
	}

	private Set<SamlGrantedAuthority> grantReadOnlyUserAuthorities() {
		final Set<SamlGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SamlGrantedAuthority(Roles.READ_ONLY_USER));

		authorities.add(new SamlGrantedAuthority(Roles.READ_ALL));

		authorities.add(new SamlGrantedAuthority(Roles.SECTION_ASSET));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_STANDARD));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REGISTER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_SUPPLIER));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_RISK_ASSESSMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_DOCUMENT));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_TASK));
		authorities.add(new SamlGrantedAuthority(Roles.SECTION_REPORT));
		return authorities;
	}
}
