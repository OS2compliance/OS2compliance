package dk.digitalidentity.model.dto;

import org.apache.commons.lang3.StringUtils;

/**
 * Which date the from/to range on the incident log filters on.
 * <p>
 * Either one of the two built-in timestamps, or the answer date of a specific
 * {@link dk.digitalidentity.model.entity.IncidentField} of type
 * {@link dk.digitalidentity.model.entity.enums.IncidentType#DATE}.
 *
 * @param target which kind of date to filter on
 * @param fieldId the incident field to read the date from, only set when target is {@link Target#FIELD}
 */
public record IncidentDateFilter(Target target, Long fieldId) {

    public enum Target {
        CREATED, UPDATED, FIELD
    }

    /**
     * Prefix used by the frontend for anything addressing a custom incident field, e.g. {@code field_42}.
     * Custom fields are addressed by id, never by their display name, because the display name
     * ({@code IncidentField.indexColumnName}) is free text an administrator can rename at any time.
     */
    public static final String FIELD_PREFIX = "field_";

    public static final IncidentDateFilter DEFAULT = new IncidentDateFilter(Target.CREATED, null);

    /**
     * Parses the {@code dateField} request parameter. Anything unrecognised falls back to
     * {@link #DEFAULT} so a stale value in the user's localStorage cannot break the page.
     */
    public static IncidentDateFilter parse(final String raw) {
        if (StringUtils.isBlank(raw) || Target.CREATED.name().equalsIgnoreCase(raw)) {
            return DEFAULT;
        }
        if (Target.UPDATED.name().equalsIgnoreCase(raw)) {
            return new IncidentDateFilter(Target.UPDATED, null);
        }
        final Long fieldId = parseFieldId(raw);
        return fieldId != null ? new IncidentDateFilter(Target.FIELD, fieldId) : DEFAULT;
    }

    /**
     * Reads the field id out of a {@code field_<id>} key, or null if the key is not one.
     * A run of digits too long for a {@code long} counts as "not one" — it cannot name a real field,
     * and the parameter comes straight off the query string.
     */
    public static Long parseFieldId(final String key) {
        if (key == null || !key.startsWith(FIELD_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(key.substring(FIELD_PREFIX.length()));
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
