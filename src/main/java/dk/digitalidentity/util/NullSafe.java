package dk.digitalidentity.util;

import java.util.function.Supplier;

public class NullSafe {

    public static <T> T nullSafe(final Supplier<T> supplier) {
        return nullSafe(supplier, null);
    }

    public static <T> T nullSafe(final Supplier<T> supplier, final T defaultValue) {
        try {
            return supplier.get();
        } catch (final NullPointerException ignored) {}
        return defaultValue;
    }

}
