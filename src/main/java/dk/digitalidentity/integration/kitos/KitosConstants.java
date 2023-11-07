package dk.digitalidentity.integration.kitos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public interface KitosConstants {

    String DEFAULT_BASE_PATH = "https://kitos.dk";

    String IT_SYSTEM_OFFSET_SETTING_KEY = "kitos_itsystems_offset";
    String IT_CONTRACT_OFFSET_SETTING_KEY = "kitos_itcontracts_offset";
    String IT_SYSTEM_USAGE_OFFSET_SETTING_KEY = "kitos_itsystem_usage_offset";

    String KITOS_UUID_PROPERTY_KEY = "kitos_uuid";
    String KITOS_OWNER_ROLE_SETTING_KEY = "kitos_owner_role_uuid";
    String KITOS_RESPONSIBLE_ROLE_SETTING_KEY = "kitos_responsible_role_uuid";

    ZonedDateTime KITOS_DELTA_START_FROM = OffsetDateTime.of(1970, 1, 1, 0, 0, 0 ,0, ZoneOffset.UTC).toZonedDateTime();

    Integer PAGE_SIZE = 100;

    Integer MAX_PAGE_REQUEST = 50;

    // How many seconds before expiry should we fetch a new token=
    Integer AUTH_S_BEFORE_EXPIRE = 30;

}
