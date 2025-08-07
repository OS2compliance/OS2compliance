package dk.digitalidentity.security;

public interface Roles {
	String AUTHENTICATED = "AUTHENTICATED";
	String API_ACCESS = "API_ACCESS";

	String CREATE_OWNER_ONLY = "ROLE_create_owner";// Limits permissions to entities that they own/are responsible for
	String DELETE_OWNER_ONLY = "ROLE_delete_owner";// Limits permissions to entities that they own/are responsible for
	String UPDATE_OWNER_ONLY = "ROLE_update_owner";// Limits permissions to entities that they own/are responsible for
	String READ_OWNER_ONLY = "ROLE_read_owner"; // Limits permissions to entities that they own/are responsible for

	String CREATE_ALL = "ROLE_create_all";
	String DELETE_ALL = "ROLE_delete_all";
	String UPDATE_ALL = "ROLE_update_all";
	String READ_ALL = "ROLE_read_all";

	String SECTION_CONFIGURATION = "ROLE_configuration";
	String SECTION_ADMIN = "ROLE_admin";
	String SECTION_ASSET = "ROLE_asset";
	String SECTION_STANDARD = "ROLE_standard";
	String SECTION_REGISTER = "ROLE_register";
	String SECTION_SUPPLIER = "ROLE_supplier";
	String SECTION_RISK_ASSESSMENT = "ROLE_risk_assessment";
	String SECTION_DOCUMENT = "ROLE_document";
	String SECTION_TASK = "ROLE_task";
	String SECTION_REPORT = "ROLE_report";



}
