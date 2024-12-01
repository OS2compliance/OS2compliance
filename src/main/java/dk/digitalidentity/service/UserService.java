package dk.digitalidentity.service;

import dk.digitalidentity.config.GRComplianceConfiguration;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final GRComplianceConfiguration configuration;
    private final UserDao userDao;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendForgottenPasswordMail(final User user) {
        user.setPasswordResetRequestDate(LocalDateTime.now());
        user.setPasswordResetToken(UUID.randomUUID().toString());
        eventPublisher.publishEvent(EmailEvent.builder()
            .message("Vi har modtaget en anmodning om at nulstille dit password. Hvis det var dig, kan du nulstille dit password ved at klikke på nedenstående link:<br>" +
                "<a href=\"" + configuration.getBaseUrl() + "/reset/" + user.getPasswordResetToken() + "\">[Nulstil dit password]</a><br>" +
                "Dette link er gyldigt i 1 time og udløber derefter automatisk. Hvis du ikke har anmodet om nulstilling af dit password, kan du ignorere denne e-mail.<br>" +
                "Hvis du har spørgsmål eller har brug for hjælp, er du velkommen til at kontakte vores supportteam på [support-e-mail/telefonnummer].<br>" +
                "Venlig hilsen<br>" +
                "GRCompliance")
            .subject("Anmodning om nulstilling af password")
            .email(user.getEmail())
            .build());
    }

    @Transactional
    public void setPassword(final User user, final String password) {
        final String encryptedPassword = new BCryptPasswordEncoder().encode(password);
        user.setPassword(encryptedPassword);
    }

    public boolean isValidPassword(final String password) {
        return StringUtils.length(password) >= 15
            && !StringUtils.isAllLowerCase(password)
            && !StringUtils.isAllUpperCase(password)
            && !StringUtils.isBlank(password)
            && !StringUtils.isAlpha(password)
            && !StringUtils.isNumericSpace(password);

    }

    public Optional<User> findNonExpiredByPasswordResetToken(final String token) {
        return userDao.findByPasswordResetToken(token)
            .filter(u -> u.getPasswordResetRequestDate().plusHours(1).isAfter(LocalDateTime.now()));
    }

    public User currentUser() {
        final String loggedInUserUuid = SecurityUtil.getLoggedInUserUuid();
        if (loggedInUserUuid != null) {
            return userDao.findById(loggedInUserUuid).orElse(null);
        }
        return null;
    }

    public Optional<User> get(final String uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        return userDao.findById(uuid);
    }

    public List<User> getAll() {
        return userDao.findAll();
    }

    public Optional<User> findByUuid(final String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return Optional.empty();
        }
        return userDao.findByUuidAndActiveIsTrue(uuid);
    }

    public Optional<User> findByUuidIncludingInactive(final String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return Optional.empty();
        }
        return userDao.findById(uuid);
    }

    public Optional<User> findByUserId(final String userId) {
        if (StringUtils.isEmpty(userId)) {
            return Optional.empty();
        }
        return userDao.findByUserIdAndActiveIsTrue(userId);
    }

    public List<User> findAllByUuids(final Set<String> userUuids) {
        return userDao.findAllByUuidInAndActiveTrue(userUuids);
    }

    public List<User> findByName(final String name) {
        return userDao.findByNameEqualsIgnoreCaseAndActiveIsTrue(name);
    }

    public List<User> findByPropertyKeyValue(final String propertyKey, final String propertyValue) {
        return userDao.findByPropertyKeyValue(propertyKey, propertyValue);
    }

    public Optional<User> findByEmail(final String email) {
        return userDao.findFirstByEmailEqualsIgnoreCaseAndActiveIsTrue(email);
    }

    public Page<User> getPaged(final int pageSize, final int page) {
        return userDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    /**
     * Persists a user to the database, updating or creating as required
     *
     * @param user
     * @return the created or updated User. Never null.
     * @throws IllegalArgumentException in case the given user is null
     */
    @Transactional
    public User save(final User user) throws IllegalArgumentException {
        return userDao.save(user);
    }

    /**
     * Deletes a user from persistence
     * @param user
     */
    @Transactional
    public void delete(User user) {
        userDao.delete(user);
    }
}
