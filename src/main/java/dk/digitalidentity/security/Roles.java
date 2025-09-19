package dk.digitalidentity.security;

public interface Roles {
	String AUTHENTICATED = "AUTHENTICATED";
	String API_ACCESS = "API_ACCESS";

	String ADMINISTRATOR = "ROLE_administrator";
	String SUPER_USER = "ROLE_super_user";
	String USER = "ROLE_user";
	String LIMITED_USER = "ROLE_limited_user";
	String READ_ONLY_USER = "ROLE_read_only";

	String CREATE_OWNER_ONLY = "ROLE_create_owner";// Limits permissions to entities that they own/are responsible for
	String DELETE_OWNER_ONLY = "ROLE_delete_owner";// Limits permissions to entities that they own/are responsible for
	String UPDATE_OWNER_ONLY = "ROLE_update_owner";// Limits permissions to entities that they own/are responsible for
	String READ_OWNER_ONLY = "ROLE_read_owner"; // Limits permissions to entities that they own/are responsible for

	String CREATE_ALL = "ROLE_create_all";
	String DELETE_ALL = "ROLE_delete_all";
	String UPDATE_ALL = "ROLE_update_all";
	String READ_ALL = "ROLE_read_all";

	String SECTION_DASHBOARD = "ROLE_s_dashboard";
	String SECTION_STANDARD = "ROLE_s_standard";
	String SECTION_REGISTER = "ROLE_s_register";
	String SECTION_ASSET = "ROLE_s_asset";
	String SECTION_DBS = "ROLE_s_dbs";
	String SECTION_SUPPLIER = "ROLE_s_supplier";
	String SECTION_RISK_ASSESSMENT = "ROLE_s_risk";
	String SECTION_DPIA = "ROLE_s_dpia";
	String SECTION_DOCUMENT = "ROLE_s_document";
	String SECTION_TASK = "ROLE_s_task";
	String SECTION_INCIDENT = "ROLE_s_incident";
	String SECTION_CONFIGURATION = "ROLE_s_configuration";
	String SECTION_REPORT = "ROLE_s_report";
	String SECTION_ADMIN = "ROLE_s_admin";
	String SECTION_SETTINGS = "ROLE_s_settings";





}
