package dk.digitalidentity.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VersionService {
	private static final String CHANGELOG_VERSION_PREFIX = "## [";
	private static final String UNRELEASED = "Unreleased";
	private static final DateTimeFormatter BUILD_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	private final GitProperties git;
	private final String changelogVersion;

	public VersionService(ObjectProvider<GitProperties> gitProperties) {
		this.git = gitProperties.getIfAvailable();
		this.changelogVersion = readLatestChangelogVersion();
	}

	/**
	 * The latest released version in the changelog, followed by the branch when not built from a tag, e.g. 2.8.0 (develop)
	 */
	public String getVersion() {
		String branch = git != null ? git.getBranch() : null;
		boolean hasBranch = branch != null && !branch.isBlank() && !"HEAD".equals(branch); // HEAD = detached, e.g. a tag build

		if (changelogVersion != null) {
			return hasBranch ? changelogVersion + " (" + branch + ")" : changelogVersion;
		}
		if (hasBranch) {
			return branch;
		}

		return git != null ? git.getShortCommitId() : null;
	}

	public String getVersionInfo() {
		if (git == null) {
			return null;
		}

		Instant instant = git.getInstant("build.time");
		String buildTime = instant != null ? BUILD_TIME_FORMAT.format(instant.atZone(ZoneId.systemDefault())) : "ukendt";

		return "Bygget: " + buildTime + "\nCommit: " + git.getShortCommitId();
	}

	private static String readLatestChangelogVersion() {
		try (InputStream in = new ClassPathResource("CHANGELOG.md").getInputStream()) {
			for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				if (line.startsWith(CHANGELOG_VERSION_PREFIX)) {
					String version = line.substring(CHANGELOG_VERSION_PREFIX.length(), line.indexOf(']'));
					if (!UNRELEASED.equalsIgnoreCase(version)) {
						return version;
					}
				}
			}
		}
		catch (IOException | RuntimeException e) {
			log.warn("Could not read version from CHANGELOG.md", e);
		}

		return null;
	}
}
