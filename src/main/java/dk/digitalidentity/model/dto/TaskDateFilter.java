package dk.digitalidentity.model.dto;

public enum TaskDateFilter {
    DEADLINE, LAST_COMPLETION;

    public static TaskDateFilter parse(final String raw) {
        if (LAST_COMPLETION.name().equalsIgnoreCase(raw)) {
            return LAST_COMPLETION;
        }
        return DEADLINE;
    }
}
