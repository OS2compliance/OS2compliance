package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.entity.enums.AssetOversightStatus;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Entity
@Table(name = "assets_oversight")
@Getter
@Setter
public class AssetOversight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;
    @Column
    @Enumerated(EnumType.STRING)
    private ChoiceOfSupervisionModel supervisionModel;
    @Column
    private String conclusion;
    @Column
    private String dbsLink;
    @Column
    private String internalDocumentationLink;
    @Column
    @Enumerated(EnumType.STRING)
    private AssetOversightStatus status;
    @ManyToOne
    @JoinColumn(name = "asset_id")
    @JsonIgnore
    private Asset asset;
    @Column(name = "creation_date")
    private LocalDate creationDate;
    @Column(name = "next_inspection_deadline")
    private LocalDate newInspectionDate;
    @Column(name = "localized_enums")
    private String localizedEnums;
}
