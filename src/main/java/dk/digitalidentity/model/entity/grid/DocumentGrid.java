package dk.digitalidentity.model.entity.grid;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_gridjs_documents")
@Getter
@Setter
@Immutable
public class DocumentGrid implements HasSingleResponsibleUser{
    @Id
    private Long id;

    @Column
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "document_type_order")
    private Integer documentTypeOrder;

    @ManyToOne
    @JoinColumn(name = "responsible_uuid")
    private User responsibleUser;

    @Column
    private LocalDateTime nextRevision;

    @Column
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column
    private Integer statusOrder;

    @Column
    private String localizedEnums;

    @Column
    private String tags;
}
