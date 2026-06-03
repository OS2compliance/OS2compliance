package dk.digitalidentity;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public interface Constants {

    String SCALE_COLOR_STD_GREEN = "#87AD27";
    String SCALE_COLOR_STD_YELLOW = "#FFDE07";
    String SCALE_COLOR_STD_RED = "#DF5645";
    String SCALE_COLOR_LIGHT_GREEN = "#93D259";
    String SCALE_COLOR_GREEN = "#1DB255";
    String SCALE_COLOR_YELLOW = "#FDFF3C";
    String SCALE_COLOR_ORANGE = "#FCC231";
    String SCALE_COLOR_RED = "#FA0020";

    String ASSET_ASSESSMENT_PROPERTY = "asset_assessment";
    String NEEDS_CVR_UPDATE_PROPERTY = "cvr_update";
    String CVR_UPDATED_PROPERTY = "cvr_updated_at";
    String ASSOCIATED_DOCUMENT_PROPERTY = "linked_doc";
    String ASSOCIATED_THREAT_ASSESSMENT_PROPERTY = "linked_threat";
    String ASSOCIATED_ASSET_DPIA_PROPERTY = "linked_dpia";
    String ASSOCIATED_INSPECTION_PROPERTY = "linked_asset";
    ZoneId LOCAL_TZ_ID = ZoneId.of("Europe/Copenhagen");
    String SYSTEM_USERID = "System";
    String DATA_MIGRATION_VERSION_SETTING = "seed_version";

    String RISK_ASSESSMENT_TEMPLATE_DOC = "reports/risk/main.docx";
    String ISO27002_REPORT_TEMPLATE_DOC = "reports/ISO27002/ISO27002.docx";
    String ISO27001_REPORT_TEMPLATE_DOC = "reports/ISO27001/ISO27001.docx";
    String ARTICLE_30_REPORT_TEMPLATE_DOC = "reports/article30/main.docx";
	String STANDARD_TEMPLATE_DOC = "reports/default/default.docx";

	String RISK_ASSESSMENT_USE_RESIDUAL = "riskAssessmentUseResidual";
	String RISK_MATRIX_USE_RESIDUAL = "riskMatrixUseResidual";
    String RISK_SCALE_PROPERTY_NAME = "riskScale";
    String LAST_NOTIFY_RUN_DAY_SETTING = "lastNotifyRunDay";
	String ALLOW_MULTIPLE_RESPONSIBLE_ON_TASKS = "allowMultipleResponsibleOnTasks";

    String CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID = "asset-type-it-system-123456";
    String CHOICE_LIST_TASK_RESULT_NO_ERROR_ID = "control-result-no-error-123456";
    String CHOICE_LIST_REGISTER_STATUS_NOT_STARTED_ID = "register-status-not-started-123456";

	String CHOICE_MEASURE_VALUE_IDENTIFIERS = "relevance-yes,relevance-no,relevance-needs-clarification,relevance-not-relevant,operation-1,operation-2,operation-3,operation-4,cloud-1,cloud-2,cloud-3,cloud-4,cloud-5,frequency-1,frequency-2,frequency-3,frequency-4,duration-1,duration-2,duration-3,duration-4";

	String DBS_OVERSIGHT_RECIPIENT_SETTING = "dbsOversightRecipient";

    DateTimeFormatter DK_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(YEAR)
            .toFormatter();

}
