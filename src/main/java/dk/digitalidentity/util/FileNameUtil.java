package dk.digitalidentity.util;

public final class FileNameUtil {

	private FileNameUtil() {
	}

	public static String sanitize(final String fileName) {
		if (fileName == null) {
			return "";
		}

		// Unicode letters are kept so Danish characters survive in the file name
		return fileName.replaceAll("[^\\p{L}\\p{N}\\s()\\-]", "_").trim();
	}
}
