package dk.digitalidentity.util;

public abstract class ComplianceStringUtils {

    public static int asNumber(final String value) {
        final String digits = value.replaceAll("[^0-9]", "");
        return Integer.parseInt(digits);
    }

}
