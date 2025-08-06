package dk.digitalidentity.integration.kitos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public interface KitosConstants {

    String IT_SYSTEM_OFFSET_SETTING_KEY = "kitos_itsystems_offset";
    String IT_CONTRACT_OFFSET_SETTING_KEY = "kitos_itcontracts_offset";
    String IT_SYSTEM_USAGE_OFFSET_SETTING_KEY = "kitos_itsystem_usage_offset";
    String USAGE_DELETION_OFFSET_USAGE_SETTING_KEY = "kitos_deletion_usage_offset";
    String IT_SYSTEM_DELETION_OFFSET_USAGE_SETTING_KEY = "kitos_deletion_it_system_offset";

    String KITOS_UUID_PROPERTY_KEY = "kitos_uuid";
    String KITOS_USAGE_UUID_PROPERTY_KEY = "kitos_usage_uuid";
	String KITOS_RISK_LAST_SYNC_PROPERTY_KEY = "kitos_risk_last_sync";
	String KITOS_DPIA_LAST_SYNC_PROPERTY_KEY = "kitos_dpia_last_sync";
    String KITOS_OWNER_ROLE_SETTING_KEY = "kitos_owner_role_uuid";
    String KITOS_OWNER_ROLE_SETTING_INPUT_FIELD_NAME = "kitos_owner_role_input_field_name";
    String KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_KEY = "kitos_operation_responsible_role_uuid";
    String KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME = "kitos_operation_responsible_role_input_field_name";
    String KITOS_RESPONSIBLE_ROLE_SETTING_KEY = "kitos_responsible_role_uuid";
    String KITOS_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME = "kitos_responsible_role_input_field_name";

    String KITOS_FIELDS_ASSET_LINK_SOURCE = "kitos_fields_asset_link_source";
    String KITOS_FIELDS_CONTRACT_DATE = "kitos_fields_contract_date";
    String KITOS_FIELDS_CONTRACT_END= "kitos_fields_contract_end";

	String KITOS_ASSET_RISK_CHANGED_QUEUE = "kitosRiskQueue";
	String KITOS_ASSET_DPIA_CHANGED_QUEUE = "kitosDPIAQueue";

    ZonedDateTime KITOS_DELTA_START_FROM = OffsetDateTime.of(1970, 1, 1, 0, 0, 0 ,0, ZoneOffset.UTC).toZonedDateTime();
    OffsetDateTime KITOS_DELTA_START_FROM_OFFSET = KITOS_DELTA_START_FROM.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);

    String IT_SYSTEM_USAGE_ENTITY_TYPE = "ItSystemUsage";
    String IT_SYSTEM_ENTITY_TYPE = "ItSystem";

    Integer PAGE_SIZE = 100;

    Integer MAX_PAGE_REQUEST = 50;


}
