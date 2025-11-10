package dk.digitalidentity.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.Constants;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.dao.SettingDao;
import dk.digitalidentity.model.entity.Setting;

@Service
public class SettingsService {
	
	@Autowired
	private SettingDao settingDao;

	@Autowired
	private TaskService taskService;

	@Autowired
	private UserService userService;

	public int getInt(final String key, final int defaultVal) {
		return settingDao.findBySettingKey(key)
            .filter(v -> v.getSettingValue() != null)
            .map((v -> Integer.parseInt(v.getSettingValue()))).orElse(defaultVal);
	}

	public String getString(final String key, final String defaultVal) {
		String settingString = settingDao.findBySettingKey(key).map(Setting::getSettingValue).orElse(defaultVal);		
		if (settingString == null || settingString.trim().isEmpty()) {
			return defaultVal;
		}

		return settingString;
	}

    public ZonedDateTime getZonedDateTime(final String key, final ZonedDateTime defaultVal) {
        return settingDao.findBySettingKey(key)
            .map(Setting::getSettingValue)
            .map(v -> OffsetDateTime.parse(v).toZonedDateTime())
            .orElse(defaultVal);
    }

	public Setting setInt(final String key, final int value) {
		return setString(key,String.valueOf(value));
	}

    public Setting setZonedDateTime(final String key, final ZonedDateTime zonedDateTime) {
        return setString(key, zonedDateTime.toOffsetDateTime().toString());
    }

	public Setting setString(final String key, final String value) {
		if (settingDao.existsBySettingKey(key)) {
			final Setting result = settingDao.findBySettingKey(key).get();
			
			result.setLastUpdated(LocalDateTime.now());
			result.setSettingValue(value);
			
			return settingDao.save(result);
		}
		else {
			final Setting setting = new Setting();
			
			setting.setSettingKey(key);
			setting.setSettingValue(value);
			setting.setLastUpdated(LocalDateTime.now());
			
			return settingDao.save(setting);
		}
	}

    public List<Setting> getByEditable(){
        return settingDao.findByEditableTrue();
    }

    public List<Setting> getByAssociationAndEditable(final String association) {
        return settingDao.findByAssociationAndEditableTrue(association);
    }

    public List<Setting> getByAssociation (final String association) {
        return  settingDao.findByAssociation(association);
    }

    //association should probably be an enum
    public Setting createSetting(final String key, final String value, final String association, final boolean editable){
        if (!settingDao.existsBySettingKey(key)) {
            final Setting setting = new Setting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setEditable(editable);
            setting.setAssociation(association);
            setting.setLastUpdated(LocalDateTime.now());

            return settingDao.save(setting);
        }
        
        return null;
    }

	/** Returns null if a setting with the key already exists*/
	public Setting createSetting(final String key, final int value) {
		return createSetting(key, String.valueOf(value));
	}

	/** Returns null if a setting with the key already exists*/
	public Setting createSetting(final String key, final String value) {
		return createSetting(key, value, null, false);
	}

	public List<Setting> saveAll(final List<Setting> settings) {

		for(final Setting setting : settings) {
			setString(setting.getSettingKey(), setting.getSettingValue());
		}
		
		return this.getAll();
	}
	
	public List<Setting> getAll() {
		return settingDao.findAll();
	}

	public void deleteSetting(final String key) {
		final Optional<Setting> setting = settingDao.findBySettingKey(key);
        setting.ifPresent(value -> settingDao.delete(value));
	}

	public Setting findBySettingKey(final String key) {
		return settingDao.findBySettingKey(key).orElse(null);
	}

	public boolean existsBySettingKey(final String key) {
		return settingDao.existsBySettingKey(key);
	}

	@Transactional
	public void updateAllowMultipleResponsible(boolean allowMultiple) {
		Setting setting = findBySettingKey(Constants.ALLOW_MULTIPLE_RESPONSIBLE_ON_TASKS);
		boolean wasMultiple = Boolean.parseBoolean(setting.getSettingValue());

		if (wasMultiple && !allowMultiple) {
			// Switching from multiple to single - preserve all users, keep only first
			List<Task> tasksWithMultipleUsers = taskService.findAll().stream()
					.filter(t -> t.getResponsibleUsers().size() > 1)
					.collect(Collectors.toList());

			for (Task task : tasksWithMultipleUsers) {
				// Save all previous users in case we switch back
				task.setPreservedResponsibleUserUuids(
						task.getResponsibleUsers().stream()
								.map(User::getUuid)
								.collect(Collectors.joining(","))
				);

				// Keep only the first user
				User firstUser = task.getResponsibleUsers().stream()
						.min(Comparator.comparing(User::getName))
						.orElse(null);

				if (firstUser != null) {
					task.getResponsibleUsers().clear();
					task.getResponsibleUsers().add(firstUser);
				}
			}

			taskService.saveAll(tasksWithMultipleUsers);
		} else if (!wasMultiple && allowMultiple) {
			// Switching from single to multiple - restore preserved users if available
			List<Task> tasksWithPreservedUsers = taskService.findAll().stream()
					.filter(t -> t.getPreservedResponsibleUserUuids() != null
							&& !t.getPreservedResponsibleUserUuids().isEmpty())
					.collect(Collectors.toList());

			for (Task task : tasksWithPreservedUsers) {
				// Restore preserved users
				Set<User> preservedUsers = Arrays.stream(task.getPreservedResponsibleUserUuids().split(","))
						.map(userService::findByUuid)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(Collectors.toSet());

				if (!preservedUsers.isEmpty()) {
					task.setResponsibleUsers(preservedUsers);
					task.setPreservedResponsibleUserUuids(null);
				}
			}

			taskService.saveAll(tasksWithPreservedUsers);
		}

		setting.setSettingValue(String.valueOf(allowMultiple));
		saveAll(Collections.singletonList(setting));
	}
}
