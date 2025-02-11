package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum NotificationSetting {
    SEVENDAYSBEFORE("notification_interval_7before"),
    ONEDAYBEFORE("notification_interval_1before"),
    ONDAY("notification_interval_onday"),
    EVERYSEVENDAYSAFTER("notification_interval_every7after");

    private final String value;

    NotificationSetting(String value) {this.value = value;}
}
