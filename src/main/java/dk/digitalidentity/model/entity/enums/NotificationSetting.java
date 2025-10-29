package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum NotificationSetting {
	ONEMONTHBEFORE("notification_interval_31before", "1 måned før", 31),
	SEVENDAYSBEFORE("notification_interval_7before", "1 uge før", 7),
    ONEDAYBEFORE("notification_interval_1before", "1 dag før", 1),
    ONDAY("notification_interval_onday", "På dagen", 0),
    EVERYSEVENDAYSAFTER("notification_interval_every7after", "Alle 7 dage efter", -7);

    private final String value;
    private final String message;
    private final Integer daysBefore;

    NotificationSetting(String value, String message, Integer daysBefore) {
		this.value = value;
		this.message = message;
		this.daysBefore = daysBefore;
	}
}
