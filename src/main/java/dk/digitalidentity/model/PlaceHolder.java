package dk.digitalidentity.model;

import lombok.Getter;

@Getter
public enum PlaceHolder {
    MUNICIPAL_NAME("{kommune}"),
    DATE("{dato}"),
    ACTIVITIES("{behandlingsaktiviteter}"),
    STANDARDS("{standards}"),
    ISO27002("{iso27002}"),
    RISK_ASSESSMENT("{risk_assessment}"),
	DATARESPONSIBLE_SETTINGS("{dataresponsible_settings}"),
	STANDARD_NAME("{standard_id}"),;

    private final String placeHolder;

    PlaceHolder(final String placeHolder) {
        this.placeHolder = placeHolder;
    }
}
