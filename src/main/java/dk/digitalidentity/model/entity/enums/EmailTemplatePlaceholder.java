package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum EmailTemplatePlaceholder {
    RECEIVER_PLACEHOLDER("{modtager}", "Navnet på modtageren"),
    OBJECT_PLACEHOLDER("{objekt}", "Navnet på objektet, det drejer sig om"),
    LINK_PLACEHOLDER("{link}", "Link til en relevant side"),
    MESSAGE_FROM_SENDER("{besked}", "Besked fra afsender"),
    DAYS_TILL_DEADLINE("{dage}", "Antal dage til deadline"),
    USER_LIST("{brugere}", "Liste over de brugere, som mailen drejer sig om"),
    SENDER("{afsender}", "Navnet på brugeren, der sender mailen");

    private final String placeholder;
    private final String description;

    private EmailTemplatePlaceholder(String placeholder, String description) {
        this.placeholder = placeholder;
        this.description = description;
    }
}
