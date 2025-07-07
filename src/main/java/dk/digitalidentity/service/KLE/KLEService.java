package dk.digitalidentity.service.KLE;

import dk.digitalidentity.kle_client.KLEClient;
import dk.digitalidentity.model.entity.KLE.KLEGroup;
import dk.digitalidentity.model.entity.KLE.KLELegalReference;
import dk.digitalidentity.model.entity.KLE.KLEMainGroup;
import dk.digitalidentity.model.entity.KLE.KLESubject;
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
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
@Service
public class KLEService {

	private final KLEClient kLEClient;

	public KLEEmneplanKomponent fetchAllFromApi() {
		try {
			return kLEClient.getEmnePlan();
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Transactional
	public List<KLEMainGroup> mapToEntities(KLEEmneplanKomponent emneplan) {
		return emneplan.getHovedgruppe().stream().map(this::mapToMainGroup).toList();
	}

	public KLEMainGroup mapToMainGroup(HovedgruppeKomponent hovedgruppe) {
		Optional<XMLGregorianCalendar> lastChanged = hovedgruppe.getHovedgruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		return new KLEMainGroup(
				hovedgruppe.getHovedgruppeNr(),
				hovedgruppe.getHovedgruppeTitel(),
				vejledningToString(hovedgruppe.getHovedgruppeVejledning()),
				gregorianToLocalDate(hovedgruppe.getHovedgruppeAdministrativInfo().getOprettetDato()),
				lastChanged.map(this::gregorianToLocalDate).orElse(null),
				hovedgruppe.getUUID(),
				hovedgruppe.getGruppe().stream().map(this::mapToGroup).toList()
		);
	}

	public KLEGroup mapToGroup(GruppeKomponent gruppe) {
		Optional<XMLGregorianCalendar> lastChanged = gruppe.getGruppeAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		return KLEGroup.builder()
				.groupNumber(gruppe.getGruppeNr())
				.title(gruppe.getGruppeTitel())
				.instructionText(vejledningToString(gruppe.getGruppeVejledning()))
				.creationDate(gregorianToLocalDate(gruppe.getGruppeAdministrativInfo().getOprettetDato()))
				.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
				.uuid(gruppe.getUUID())
				.subjects(gruppe.getEmne().stream().map(this::mapToSubject).toList())
				.legalReferences(gruppe.getGruppeRetskildeReference().stream().map(this::mapToLegalReference).toList())
				.build();
	}

	public KLESubject mapToSubject(EmneKomponent emne) {
		Optional<XMLGregorianCalendar> lastChanged = emne.getEmneAdministrativInfo().getRettetDato().stream().max(XMLGregorianCalendar::compare);
		return KLESubject.builder()
				.subjectNumber(emne.getEmneNr())
				.title(emne.getEmneTitel())
				.instructionText(vejledningToString(emne.getEmneVejledning()))
				.creationDate(gregorianToLocalDate(emne.getEmneAdministrativInfo().getOprettetDato()))
				.lastUpdateDate(lastChanged.map(this::gregorianToLocalDate).orElse(null))
				.preservationCode(emne.getBevaringJaevnfoerArkivloven())
				.durationBeforeDeletion(fromXmlDuration(emne.getSletningJaevnfoerPersondataloven()))
				.uuid(emne.getUUID())
				.legalReferences(emne.getEmneRetskildeReference().stream().map(this::mapToLegalReference).toList())
				.build();
	}

	public KLELegalReference mapToLegalReference(RetskildeReferenceKomponent retskilde) {
		return KLELegalReference.builder()
				.accessionNumber(retskilde.getRetsinfoAccessionsNr())
				.paragraph(retskilde.getParagrafEllerKapitel())
				.url(retskilde.getRetsinfoURL())
				.title(retskilde.getRetskildeTitel())
				.build();
	}

	public LocalDate gregorianToLocalDate(XMLGregorianCalendar gregorianCalendar) {
		return LocalDate.of(gregorianCalendar.getYear(), gregorianCalendar.getMonth(), gregorianCalendar.getDay());
	}

	public String vejledningToString(VejledningKomponent vejledningKomponent) {
		return String.join("<br>", vejledningKomponent.getVejledningTekst().getP().stream().flatMap(p -> p.getContent().stream()).map(Object::toString).toList());
	}

	public static Duration fromXmlDuration(javax.xml.datatype.Duration xmlDuration) {
		if (xmlDuration == null) return null;
		try {
			// Method 1: Use ISO-8601 string (most accurate)
			return Duration.parse(xmlDuration.toString());
		} catch (Exception e) {
			// Fallback: Use time in milliseconds
			long millis = xmlDuration.getTimeInMillis(new Date());
			return Duration.ofMillis(millis);
		}
	}
}
