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
	UNKNOWN("Ukendt"),
	PRESERVEDATACANDISCARDDOCUMENTS("Bevar data, dokumenter kan kasseres");

	private final String message;

	public static ArchiveDuty fromApiEnum(ArchivingRegistrationsResponseDTO.ArchiveDutyEnum apiEnum) {
		if (apiEnum == null) return null;
		return switch (apiEnum) {
			case UNDECIDED -> ArchiveDuty.UNDECIDED;
			case B -> ArchiveDuty.B;
			case K -> ArchiveDuty.K;
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
			case UNKNOWN -> ArchiveDuty.UNKNOWN;
			case PRESERVEDATACANDISCARDDOCUMENTS -> ArchiveDuty.PRESERVEDATACANDISCARDDOCUMENTS;
		};
	}
}