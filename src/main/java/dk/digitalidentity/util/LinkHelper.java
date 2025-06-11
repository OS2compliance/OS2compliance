package dk.digitalidentity.util;

public class LinkHelper {

    /**
     * Add https:// if the link doesn't have a schema
     */
    public static String linkify(final String link) {
        if (link == null || link.isEmpty()) {
            return null;
        }
        if (!link.contains("://")) {
            return "https://" + link;
        }
        return link;
    }

}
