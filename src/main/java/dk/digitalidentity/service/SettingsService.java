package dk.digitalidentity.service;

import dk.digitalidentity.dao.SettingDao;
import dk.digitalidentity.model.entity.Setting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SettingsService {
	@Autowired
	SettingDao settingDao;

	public int getInt(final String key, final int defaultVal) {
		return settingDao.findBySettingKey(key)
            .filter(v -> v.getSettingValue() != null)
            .map((v -> Integer.parseInt(v.getSettingValue()))).orElse(defaultVal);
	}

	public String getString(final String key, final String defaultVal) {
		return settingDao.findBySettingKey(key).map(Setting::getSettingValue).orElse(defaultVal);
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
		} else {
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
        if(!settingDao.existsBySettingKey(key)) {
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
}
