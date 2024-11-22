package dk.digitalidentity.model.entity.grid;

import java.time.LocalDate;
import java.util.List;

import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Task;
import org.hibernate.annotations.Immutable;

import dk.digitalidentity.config.DBSAssetListConverter;
import dk.digitalidentity.model.entity.DBSAsset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetOversightStatus;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "view_gridjs_dbs_oversights")
@Getter
@Setter
@Immutable
public class DBSOversightGrid {
	@Id
	private Long id;

	@Column
	private String name;

    @Column
    private String supplier;

    @Column
    private Long supplierId;

    @Column
    @Enumerated(EnumType.STRING)
    private ChoiceOfSupervisionModel supervisoryModel;

    @Column
    @Convert(converter = DBSAssetListConverter.class)
    private List<DBSAsset> dbsAssets;

    @ManyToOne
    @JoinColumn(name = "oversight_responsible_uuid")
    private User oversightResponsible;

	@Column
	private LocalDate lastInspection;

	@Column
	@Enumerated(EnumType.STRING)
	private AssetOversightStatus lastInspectionStatus;

    @ManyToOne
    @JoinColumn(name = "outstanding_task_id")
    private Task outstandingTask;

	@Column
	private String localizedEnums;
}
