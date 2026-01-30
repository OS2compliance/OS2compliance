package dk.digitalidentity.model.entity.enums;

import dk.digitalidentity.model.api.AssetEO;
import dk.kitos.api.model.ArchivingRegistrationsResponseDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ArchiveDuty {
	UNDECIDED("Ikke besluttet"),
	B("B"),
	K("K"),
	BK("BK"),
	KD("KD"),
	KB("KB"),
	UNKNOWN("Ukendt"),
	PRESERVEDATACANDISCARDDOCUMENTS("Bevar data, dokumenter kan kasseres");

	private final String message;

	public static ArchiveDuty fromApiEnum(ArchivingRegistrationsResponseDTO.ArchiveDutyEnum apiEnum) {
		if (apiEnum == null) return null;
		return switch (apiEnum) {
			case UNDECIDED -> ArchiveDuty.UNDECIDED;
			case B -> ArchiveDuty.B;
			case K -> ArchiveDuty.K;
			case BK -> ArchiveDuty.BK;
			case KD -> ArchiveDuty.KD;
			case KB -> ArchiveDuty.KB;
			case UNKNOWN -> ArchiveDuty.UNKNOWN;
			case PRESERVEDATACANDISCARDDOCUMENTS -> ArchiveDuty.PRESERVEDATACANDISCARDDOCUMENTS;
		};
	}

	public static ArchiveDuty fromApiEnum(AssetEO.ArchiveDuty apiEnum) {
		if (apiEnum == null) return null;
		return switch (apiEnum) {
			case UNDECIDED -> ArchiveDuty.UNDECIDED;
			case B -> ArchiveDuty.B;
			case K -> ArchiveDuty.K;
			case BK -> ArchiveDuty.BK;
			case KD -> ArchiveDuty.KD;
			case KB -> ArchiveDuty.KB;
			case UNKNOWN -> ArchiveDuty.UNKNOWN;
			case PRESERVEDATACANDISCARDDOCUMENTS -> ArchiveDuty.PRESERVEDATACANDISCARDDOCUMENTS;
		};
	}
}