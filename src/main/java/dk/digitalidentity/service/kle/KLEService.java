package dk.digitalidentity.service.kle;

import dk.digitalidentity.kle_client.KLEClient;
import dk.digitalidentity.model.entity.kle.KLEGroup;
import dk.digitalidentity.model.entity.kle.KLELegalReference;
import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import dk.digitalidentity.model.entity.kle.KLESubject;
import dk.kle_online.rest.resources.full.EmneKomponent;
import dk.kle_online.rest.resources.full.GruppeKomponent;
import dk.kle_online.rest.resources.full.HovedgruppeKomponent;
import dk.kle_online.rest.resources.full.KLEEmneplanKomponent;
import dk.kle_online.rest.resources.full.RetskildeReferenceKomponent;
import dk.kle_online.rest.resources.full.VejledningKomponent;
import jakarta.xml.bind.JAXBException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Service
public class KLEService {

	private final KLEClient kLEClient;
	private final KLEMainGroupService kLEMainGroupService;
	private final KLEGroupService kLEGroupService;
	private final KLESubjectService kLESubjectService;
	private final KLELegalReferenceService kLELegalReferenceService;

	private Set<KLELegalReference> kleLegalReferenceCache = new HashSet<>();

	/**
	 * Fetches an Emneplan containing all data from KLE API
	 *
	 * @return KLEEmneplanKomponent containing all data from KLE API
	 */
	public KLEEmneplanKomponent fetchAllFromApi() throws JAXBException {
		return kLEClient.getEmnePlan();
	}

	/**
	 * Removes all KLE-related entities in database and replaces them with entities based on a fetch from KLE api
	 *
	 * @param emneplan the result of a fetch from KLE Api
	 * @return a list of MainGroups
	 */
	@Transactional
	public List<KLEMainGroup> syncToDatabase(KLEEmneplanKomponent emneplan) {

		// Map and persist
		kleLegalReferenceCache = new HashSet<>(); // Reset cache used for legalreferences
		List<KLEMainGroup> mainGroups = emneplan.getHovedgruppe().stream().map(this::mapToMainGroup).toList();

		// Delete all existing in db
		deleteUnusedFromDatabase(mainGroups);

		return kLEMainGroupService.saveAll(mainGroups);
	}

	/**
	 * Soft deletes any KLE objects in the db not matching the list of maingroup (and its content)
	 * @param mainGroups Master list of KLEMainGroups, groups and subjects
	 */
	public void deleteUnusedFromDatabase(List<KLEMainGroup> mainGroups) {
		Set<String> mainGroupNumbers = new HashSet<>();
		Set<String> groupNumbers = new HashSet<>();
		Set<String> subjectNumbers = new HashSet<>();
		Set<String> legalRefNumbers = new HashSet<>();

		for (KLEMainGroup mainGroup : mainGroups) {
			mainGroupNumbers.add(mainGroup.getMainGroupNumber());
			groupNumbers.addAll(mainGroup.getKleGroups().stream()
					.map(KLEGroup::getGroupNumber)
					.collect(Collectors.toSet()));
			subjectNumbers.addAll(mainGroup.getKleGroups().stream()
					.flatMap(g -> g.getSubjects().stream())
					.map(KLESubject::getSubjectNumber)
					.collect(Collectors.toSet()));
			//Add legal refs from groups
			legalRefNumbers.addAll(mainGroup.getKleGroups().stream()
					.flatMap(g -> g.getLegalReferences().stream())
					.map(KLELegalReference::getAccessionNumber)
					.collect(Collectors.toSet()));

			//add legal refs from subjects
			legalRefNumbers.addAll(mainGroup.getKleGroups().stream()
					.flatMap(g -> g.getSubjects().stream()
							.flatMap(s -> s.getLegalReferences().stream()))
					.map(KLELegalReference::getAccessionNumber)
					.collect(Collectors.toSet()));
		}

		kLEMainGroupService.softDeleteAllNotMatching(mainGroupNumbers);
		kLEGroupService.softDeleteAllNotMatching(groupNumbers);
		kLESubjectService.softDeleteAllNotMatching(subjectNumbers);
		kLELegalReferenceService.softDeleteAllNotMatching(legalRefNumbers);

	}

	private KLEMainGroup mapToMainGroup(HovedgruppeKomponent hovedgruppe) {
		Optional<XMLGregorianCalendar> lastChanged = hovedgruppe.getHovedgruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);

		KLEMainGroup mainGroup = KLEMainGroup.builder()
				.mainGroupNumber(hovedgruppe.getHovedgruppeNr())
				.title(hovedgruppe.getHovedgruppeTitel())
				.instructionText(vejledningToString(hovedgruppe.getHovedgruppeVejledning()))
				.creationDate(gregorianToLocalDate(hovedgruppe.getHovedgruppeAdministrativInfo().getOprettetDato()))
				.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
				.uuid(hovedgruppe.getUUID())
				.deleted(false)
				.build();

		KLEMainGroup persistedMainGroup = kLEMainGroupService.save(mainGroup);
		persistedMainGroup.setKleGroups(hovedgruppe.getGruppe().stream().map(g -> mapToGroup(g, persistedMainGroup)).collect(Collectors.toSet()));

		return persistedMainGroup;
	}

	private KLEGroup mapToGroup(GruppeKomponent gruppe, KLEMainGroup mainGroup) {
		Optional<XMLGregorianCalendar> lastChanged = gruppe.getGruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		KLEGroup kleGroup = KLEGroup.builder()
				.groupNumber(gruppe.getGruppeNr())
				.title(gruppe.getGruppeTitel())
				.instructionText(vejledningToString(gruppe.getGruppeVejledning()))
				.creationDate(gregorianToLocalDate(gruppe.getGruppeAdministrativInfo().getOprettetDato()))
				.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
				.uuid(gruppe.getUUID())
				.mainGroup(mainGroup)
				.deleted(false)
				.build();

		KLEGroup persistedGroup = kLEGroupService.save(kleGroup);
		persistedGroup.setSubjects(gruppe.getEmne().stream().map(e -> mapToSubject(e, persistedGroup)).collect(Collectors.toSet()));
		persistedGroup.setLegalReferences(gruppe.getGruppeRetskildeReference().stream().map(l -> addKLELegalReference(l, persistedGroup, null)).collect(Collectors.toSet()));
		return persistedGroup;
	}

	private KLESubject mapToSubject(EmneKomponent emne, KLEGroup group) {
		Optional<XMLGregorianCalendar> lastChanged = emne.getEmneAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		KLESubject kleSubject = KLESubject.builder()
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

		KLESubject persistedSubject = kLESubjectService.save(kleSubject);
		persistedSubject.setLegalReferences(emne.getEmneRetskildeReference().stream().map(r -> addKLELegalReference(r, null, persistedSubject)).collect(Collectors.toSet()));

		return persistedSubject;
	}

	private KLELegalReference addKLELegalReference(RetskildeReferenceKomponent kleLegalReference, KLEGroup group, KLESubject subject) {
		KLELegalReference current = kleLegalReferenceCache.stream().filter(l -> l.getAccessionNumber().equals(kleLegalReference.getRetsinfoAccessionsNr())).findAny().orElse(null);
		if (current == null) {
			current = mapToLegalReference(kleLegalReference);
			kleLegalReferenceCache.add(current);
		}
		if (group != null) {
			group.getLegalReferences().add(current);
		}
		if (subject != null) {
			subject.getLegalReferences().add(current);
		}
		current.setDeleted(false);

		return current;
	}

	private KLELegalReference mapToLegalReference(RetskildeReferenceKomponent retskilde) {
		KLELegalReference kleLegalReference = KLELegalReference.builder()
				.accessionNumber(retskilde.getRetsinfoAccessionsNr())
				.paragraph(retskilde.getParagrafEllerKapitel())
				.url(retskilde.getRetsinfoURL())
				.title(retskilde.getRetskildeTitel())
				.build();
		return kLELegalReferenceService.save(kleLegalReference);
	}

	private LocalDate gregorianToLocalDate(XMLGregorianCalendar gregorianCalendar) {
		if (gregorianCalendar == null) {
			return null;
		}
		return LocalDate.of(gregorianCalendar.getYear(), gregorianCalendar.getMonth(), gregorianCalendar.getDay());
	}

	private String vejledningToString(VejledningKomponent vejledningKomponent) {
		if (vejledningKomponent == null) {
			return "";
		}
		return String.join("<br>", vejledningKomponent.getVejledningTekst().getP().stream().flatMap(p -> p.getContent().stream()).map(Object::toString).toList());
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
