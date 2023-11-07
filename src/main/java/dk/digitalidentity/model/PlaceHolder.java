package dk.digitalidentity.model;

import lombok.Getter;

@Getter
public enum PlaceHolder {
    MUNICIPAL_NAME("{kommune}"),
    DATE("{dato}"),
    ACTIVITIES("{behandlingsaktiviteter}"),
    STANDARDS("{standards}"),
    ISO27002("{iso27002}");

    private final String placeHolder;

    PlaceHolder(final String placeHolder) {
        this.placeHolder = placeHolder;
    }
}
