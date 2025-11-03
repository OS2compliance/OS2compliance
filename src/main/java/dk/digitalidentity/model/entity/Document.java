package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.DocumentType;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.interfaces.HasSingleResponsibleUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document extends Relatable implements HasSingleResponsibleUser, Tagable {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;

    @Column
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column
    private String description;

    @Column
    private String link;

    @Column
    private String documentVersion;

    @Column
    @Enumerated(EnumType.STRING)
    private DocumentRevisionInterval revisionInterval;

    @Column
    @DateTimeFormat(pattern = "dd/MM-yyyy")
    private LocalDate nextRevision;

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
	@JoinTable(name = "document_tag", joinColumns = { @JoinColumn(name = "document_id") }, inverseJoinColumns = { @JoinColumn(name = "tag_id") })
	private Set<Tag> tags = new HashSet<>();

    @Override
    public RelationType getRelationType() {
        return RelationType.DOCUMENT;
    }

    @Override
    public String getLocalizedEnumValues() {
        return (status != null ? status.getMessage() : "") + " " +
                (documentType != null ? documentType.getMessage() : "");
    }
}
