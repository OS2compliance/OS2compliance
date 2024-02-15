package dk.digitalidentity.service;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserDao userDao;

    public UserService(final UserDao userDao) {
        this.userDao = userDao;
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

    public Optional<User> findByUuid(final String uuid) {
        if(StringUtils.isEmpty(uuid)) {
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
}
