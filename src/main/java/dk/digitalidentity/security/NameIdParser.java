package dk.digitalidentity.security;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class NameIdParser {

    public static Optional<String> parseNameId(final String nameId) {
        if (isUUID(nameId)) {
            return Optional.of(nameId);
        } else if (isX509Format(nameId)) {
            return Arrays.stream(StringUtils.split(nameId, ","))
                .filter(p -> p.contains("Serial="))
                .map(p -> StringUtils.substringAfter(p, "="))
                .findFirst();
        } else if (isBase64Binary(nameId)) {
            return Optional.ofNullable(decodeBase64BinaryUUID(nameId));
        }
        return Optional.empty();
    }

    private static boolean isUUID(final String nameId) {
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(nameId);
            return true;
        } catch (final IllegalArgumentException ignored) {}
        return false;
    }

    private static boolean isX509Format(final String nameId) {
        return StringUtils.split(nameId, ",").length > 0 && nameId.contains("Serial=");
    }

    private static boolean isBase64Binary(final String nameId) {
        try {
            final byte[] decoded = Base64.getDecoder().decode(nameId);
            return true;
        } catch (final IllegalArgumentException ignored) {}
        return false;
    }

    private static String decodeBase64BinaryUUID(final String principal) {
        final byte[] decoded = Base64.getDecoder().decode(principal);
        final int[] theByteOrder = {3,2,1,0,5,4,7,6,8,9,10,11,12,13,14,15};
        final ByteBuffer buffer = ByteBuffer.allocate(decoded.length);
        for (final int offset : theByteOrder) {
            buffer.put(decoded[offset]);
        }
        buffer.rewind();
        final long high = buffer.getLong();
        final long low = buffer.getLong();
        final UUID uuid = new UUID(high, low);
        return uuid.toString();
    }

}
