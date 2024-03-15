package dk.digitalidentity;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public interface Constants {

    String NEEDS_CVR_UPDATE_PROPERTY = "cvr_update";
    String CVR_UPDATED_PROPERTY = "cvr_updated_at";
    String ASSOCIATED_DOCUMENT_PROPERTY = "linked_doc";
    String ASSOCIATED_THREAT_ASSESSMENT_PROPERTY = "linked_threat";
    String ASSOCIATED_INSPECTION_PROPERTY = "linked_asset";
    ZoneId LOCAL_TZ_ID = ZoneId.of("Europe/Copenhagen");
    String SYSTEM_USERID = "System";
    String DATA_MIGRATION_VERSION_SETTING = "seed_version";

    String RISK_ASSESSMENT_TEMPLATE_DOC = "reports/risk/main.docx";

    String ISO27002_REPORT_TEMPLATE_DOC = "reports/ISO27002/ISO27002.docx";
    String ISO27001_REPORT_TEMPLATE_DOC = "reports/ISO27001/ISO27001.docx";
    String ARTICLE_30_REPORT_TEMPLATE_DOC = "reports/article30/main.docx";

    DateTimeFormatter DK_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(YEAR)
            .toFormatter();

}
