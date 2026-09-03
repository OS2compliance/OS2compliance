package dk.digitalidentity.integration.dbs;

public interface DBSConstants {
    String OVERSIGHT_LAST_TIMESTAMP = "oversight_last_timestamp";

	/**
	 * Vandmaerket for platform-syncen: nyeste publishedDate der er hentet ind. Naeste koersel henter
	 * kun audits udgivet efter dette tidspunkt, saa en nulstilling spoler vinduet tilbage og henter
	 * audits igen - eneste vej til at genimportere en audit der blev sprunget over.
	 */
	String PLATFORM_LAST_SYNC = "dbs_platform_last_sync";
}
