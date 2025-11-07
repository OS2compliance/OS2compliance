package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum NotificationSetting {
	ONEMONTHBEFORE("notification_interval_31before", "1 måned før"),
	SEVENDAYSBEFORE("notification_interval_7before", "1 uge før"),
    ONEDAYBEFORE("notification_interval_1before", "1 dag før"),
    ONDAY("notification_interval_onday", "På dagen"),
    EVERYSEVENDAYSAFTER("notification_interval_every7after", "Hver 7. dag efter");

    private final String value;
    private final String message;

    NotificationSetting(String value, String message) {
		this.value = value;
		this.message = message;
	}
}
