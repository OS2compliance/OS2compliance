package dk.digitalidentity.service.kle;

import dk.digitalidentity.kle_client.KLEClient;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLEKeyword;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.kle_online.rest.resources.full.EmneKomponent;
import dk.kle_online.rest.resources.full.EmneOgHandlingsfacetStikordKomponent;
import dk.kle_online.rest.resources.full.GruppeKomponent;
import dk.kle_online.rest.resources.full.HovedgruppeKomponent;
import dk.kle_online.rest.resources.full.KLEEmneplanKomponent;
import dk.kle_online.rest.resources.full.RetskildeReferenceKomponent;
import dk.kle_online.rest.resources.full.StikordKomponent;
import dk.kle_online.rest.resources.full.VejledningKomponent;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KLEService {
	private final JAXBContext jaxbContext;
	private final KLEClient kLEClient;
	private final KLEDatabaseService kLEDatabaseService;

	private final Map<String, KLEMainGroup> mainGroupCache = new ConcurrentHashMap<>();
	private final Map<String, KLEGroup> groupCache = new ConcurrentHashMap<>();
	private final Map<String, KLESubject> subjectCache = new ConcurrentHashMap<>();
	private Map<String, KLELegalReference> kleLegalReferenceCache = new ConcurrentHashMap<>();
	private Map<String, KLEKeyword> kleKeywordCache = new ConcurrentHashMap<>();
	private Marshaller htmlTextMarshaller = null;

	/**
	 * Fetches an Emneplan containing all data from KLE API
	 *
	 * @return KLEEmneplanKomponent containing all data from KLE API
	 */
	public KLEEmneplanKomponent fetchAllFromApi() throws JAXBException {
		return kLEClient.getEmnePlan();
	}

	/**
	 * Loads KLEEmneplanKomponent data from a predefined file in the classpath
	 * and synchronizes it with the database.
	 *
	 * @throws RuntimeException if an error occurs during file reading or unmarshalling.
	 *                          This may be caused by an issue with locating the file, invalid XML content,
	 *                          or any I/O issue.
	 */
	@Transactional
	public void loadFromClassPath() {
		try (InputStream emnePlanStream = this.getClass().getClassLoader().getResourceAsStream("data/kle-emneplan.xml")) {
			Unmarshaller unmarshaller = this.jaxbContext.createUnmarshaller();
			JAXBElement<KLEEmneplanKomponent> element = unmarshaller.unmarshal(new StreamSource(emnePlanStream), KLEEmneplanKomponent.class);
			syncToDatabase(element.getValue());
		}
		catch (IOException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Removes all KLE-related entities in database and replaces them with entities based on a fetch from KLE api
	 *
	 * @param emneplan the result of a fetch from KLE Api
	 */
	@Transactional
	public void syncToDatabase(KLEEmneplanKomponent emneplan) {

		// Map and persist
		kleLegalReferenceCache = new ConcurrentHashMap<>(); // Reset cache used for legalreferences
		kleKeywordCache = new ConcurrentHashMap<>(); // Reset cache used for legalreferences

		emneplan.getHovedgruppe()
				.forEach(this::mapToMainGroup);

		kLEDatabaseService.syncWithDatabase(mainGroupCache, groupCache, subjectCache, kleLegalReferenceCache, kleKeywordCache);
	}

	private void mapToMainGroup(HovedgruppeKomponent hovedgruppe) {
		Optional<XMLGregorianCalendar> lastChanged = hovedgruppe.getHovedgruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		KLEMainGroup current = mainGroupCache.get(hovedgruppe.getHovedgruppeNr());
		if (current == null) {

			current = KLEMainGroup.builder()
					.mainGroupNumber(hovedgruppe.getHovedgruppeNr())
					.title(hovedgruppe.getHovedgruppeTitel())
					.instructionText(vejledningToString(hovedgruppe.getHovedgruppeVejledning()))
					.creationDate(gregorianToLocalDate(hovedgruppe.getHovedgruppeAdministrativInfo().getOprettetDato()))
					.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
					.uuid(hovedgruppe.getUUID())
					.deleted(false)
					.build();

			mainGroupCache.put(current.getMainGroupNumber(), current);
		}
		KLEMainGroup mainGroup = current;

		mainGroup.setKleGroups(hovedgruppe.getGruppe().stream().map(g -> mapToGroup(g, mainGroup)).collect(Collectors.toSet()));
	}

	private KLEGroup mapToGroup(GruppeKomponent gruppe, KLEMainGroup mainGroup) {
		Optional<XMLGregorianCalendar> lastChanged = gruppe.getGruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		KLEGroup current = groupCache.get(gruppe.getGruppeNr());
		if (current == null) {
			current = KLEGroup.builder()
					.groupNumber(gruppe.getGruppeNr())
					.title(gruppe.getGruppeTitel())
					.instructionText(vejledningToString(gruppe.getGruppeVejledning()))
					.creationDate(gregorianToLocalDate(gruppe.getGruppeAdministrativInfo().getOprettetDato()))
					.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
					.uuid(gruppe.getUUID())
					.mainGroup(mainGroup)
					.deleted(false)
					.build();

			groupCache.put(current.getGroupNumber(), current);
		}
		KLEGroup kleGroup = current;

		kleGroup.setSubjects(gruppe.getEmne().stream().map(e -> mapToSubject(e, kleGroup)).collect(Collectors.toSet()));

		gruppe.getGruppeRetskildeReference().forEach(l -> addKLELegalReference(l, kleGroup, null));

		gruppe.getGruppeStikord().forEach(s -> addKLEkeyword(s, kleGroup, null));

		return kleGroup;
	}

	private KLESubject mapToSubject(EmneKomponent emne, KLEGroup group) {
		Optional<XMLGregorianCalendar> lastChanged = emne.getEmneAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		KLESubject current = subjectCache.get(emne.getEmneNr());
		if (current == null) {
			current = KLESubject.builder()
					.subjectNumber(emne.getEmneNr())
					.title(emne.getEmneTitel())
					.instructionText(vejledningToString(emne.getEmneVejledning()))
					.creationDate(gregorianToLocalDate(emne.getEmneAdministrativInfo().getOprettetDato()))
					.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
					.preservationCode(emne.getBevaringJaevnfoerArkivloven())
					.durationBeforeDeletion(fromXmlDuration(emne.getSletningJaevnfoerPersondataloven()))
					.uuid(emne.getUUID())
					.group(group)
					.deleted(false)
					.build();

			subjectCache.put(current.getSubjectNumber(), current);
		}

		KLESubject subject = current;
		Set<KLELegalReference> legalReferences = emne.getEmneRetskildeReference().stream().map(r -> addKLELegalReference(r, null, subject)).collect(Collectors.toSet());
		current.setLegalReferences(legalReferences);

		emne.getEmneStikord().forEach(s -> addKLEkeyword(s, null, subject));
		emne.getEmneOgHandlingsfacetStikord().forEach(s -> addKLEkeyword(s, subject));

		return current;
	}

	private KLELegalReference addKLELegalReference(RetskildeReferenceKomponent kleLegalReference, KLEGroup group, KLESubject subject) {
		KLELegalReference current = kleLegalReferenceCache.get(kleLegalReference.getRetsinfoAccessionsNr());
		if (current == null) {
			current = mapToLegalReference(kleLegalReference);
			kleLegalReferenceCache.put(current.getAccessionNumber(), current);
		}
		if (group != null) {
			if (group.getLegalReferences() == null) {
				group.setLegalReferences(new HashSet<>());
			}
			group.getLegalReferences().add(current);
			if (current.getGroups() == null) {
				current.setGroups(new HashSet<>());
			}
			current.getGroups().add(group);
			group.getLegalReferences().add(current);
		}
		if (subject != null) {
			if (subject.getLegalReferences() == null) {
				subject.setLegalReferences(new HashSet<>());
			}
			subject.getLegalReferences().add(current);
			if (current.getSubjects() == null) {
				current.setSubjects(new HashSet<>());
			}
			current.getSubjects().add(subject);
			subject.getLegalReferences().add(current);
		}
		current.setDeleted(false);

		return current;
	}

	private KLELegalReference mapToLegalReference(RetskildeReferenceKomponent retskilde) {
		return KLELegalReference.builder()
				.accessionNumber(retskilde.getRetsinfoAccessionsNr())
				.paragraph(retskilde.getParagrafEllerKapitel())
				.url(retskilde.getRetsinfoURL())
				.title(retskilde.getRetskildeTitel())
				.build();
	}

	private void addKLEkeyword(StikordKomponent stikord, KLEGroup group, KLESubject subject) {
		if (stikord == null || stikord.getTekst() == null || stikord.getTekst().isEmpty()) {
			return;
		}
		String hashedId = generateHashId(stikord.getTekst());
		KLEKeyword current = kleKeywordCache.get(hashedId);
		if (current == null) {
			current = mapToKeyword(stikord, hashedId);
			kleKeywordCache.put(hashedId, current);
		}
		if (group != null) {
			if (group.getKeywords() == null) {
				group.setKeywords(new HashSet<>());
			}
			if (current.getGroups() == null) {
				current.setGroups(new HashSet<>());
			}
			current.getGroups().add(group);
			group.getKeywords().add(current);
		}
		if (subject != null) {
			if (subject.getKeywords() == null) {
				subject.setKeywords(new HashSet<>());
			}
			if (current.getSubjects() == null) {
				current.setSubjects(new HashSet<>());
			}
			current.getSubjects().add(subject);
			subject.getKeywords().add(current);
		}

	}

	private void addKLEkeyword(EmneOgHandlingsfacetStikordKomponent stikord, KLESubject subject) {
		if (stikord == null || stikord.getTekst() == null || stikord.getTekst().isEmpty()) {
			return;
		}
		String hashedId = generateHashId(stikord.getTekst());
		KLEKeyword current = kleKeywordCache.get(hashedId);
		if (current == null) {
			current = mapToKeyword(stikord, hashedId);
			kleKeywordCache.put(hashedId, current);
		}
		if (subject != null) {
			if (subject.getKeywords() == null) {
				subject.setKeywords(new HashSet<>());
			}
			if (current.getSubjects() == null) {
				current.setSubjects(new HashSet<>());
			}
			current.getSubjects().add(subject);
			subject.getKeywords().add(current);
		}

	}

	private KLEKeyword mapToKeyword(StikordKomponent stikord, String hashedId) {
		return KLEKeyword.builder()
				.hashedId(hashedId)
				.text(stikord.getTekst())
				.handlingsfacetNr(null)
				.build();
	}

	private KLEKeyword mapToKeyword(EmneOgHandlingsfacetStikordKomponent stikord, String hashedId) {
		return KLEKeyword.builder()
				.hashedId(hashedId)
				.text(stikord.getTekst())
				.handlingsfacetNr(stikord.getHandlingsfacetNr())
				.build();
	}

	private String generateHashId(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();

			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}

			return hexString.toString();
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	private LocalDate gregorianToLocalDate(XMLGregorianCalendar gregorianCalendar) {
		if (gregorianCalendar == null) {
			return null;
		}
		return LocalDate.of(gregorianCalendar.getYear(), gregorianCalendar.getMonth(), gregorianCalendar.getDay());
	}

	private Marshaller getHtmlTextMarshaller() throws JAXBException {
		if (htmlTextMarshaller == null) {
			htmlTextMarshaller = jaxbContext.createMarshaller();
			htmlTextMarshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			htmlTextMarshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			htmlTextMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		}
		return htmlTextMarshaller;
	}

	private String vejledningToString(VejledningKomponent vejledningKomponent) {
		if (vejledningKomponent == null) {
			return "";
		}
		try {
			final StringWriter sw = new StringWriter();
			JAXBElement<VejledningKomponent.VejledningTekst> komponentJAXBElement = new JAXBElement<>(
					new QName("VejledningTekst"),
					VejledningKomponent.VejledningTekst.class,
					vejledningKomponent.getVejledningTekst()
			);

			getHtmlTextMarshaller().marshal(komponentJAXBElement, sw);
			final String fullXml = removeNamespaces(sw.toString());

			// Skræl den yderste VejledningTekst af, vi vil kun have html'en inden i
			int start = fullXml.indexOf('>') + 1;
			int end = fullXml.lastIndexOf('<');
			return start < end ? fullXml.substring(start, end).trim() : fullXml;
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private static String removeNamespaces(String xml) {
		// Step 1: Remove all namespace declarations (e.g., xmlns="...")
		xml = xml.replaceAll("xmlns(:\\w+)?=\"[^\"]*\"", "");
		// Step 2: Remove all namespace prefixes from element names (e.g., <ns:p> to <p>)
		xml = xml.replaceAll("(<|</)\\w+:", "$1");
		// Step 3: Remove unnecessary spaces after opening tags (e.g., <li > becomes <li>)
		xml = xml.replaceAll("<(\\w+)\\s+>", "<$1>");

		return xml.trim();
	}

	private static Duration fromXmlDuration(javax.xml.datatype.Duration xmlDuration) {
		if (xmlDuration == null)
			return null;
		try {
			// Method 1: Use ISO-8601 string (most accurate)
			return Duration.parse(xmlDuration.toString());
		}
		catch (Exception e) {
			// Fallback: Use time in milliseconds
			long millis = xmlDuration.getTimeInMillis(new Date());
			return Duration.ofMillis(millis);
		}
	}
}
