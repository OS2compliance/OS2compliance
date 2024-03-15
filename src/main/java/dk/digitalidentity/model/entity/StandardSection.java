package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.StandardSectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standard_sections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardSection extends Relatable {

    @OneToOne
    @JoinColumn(name = "template_section_identifier")
    private StandardTemplateSection templateSection;

    @Column
    private String description;

    @Column
    private String nsisPractice;

    @Column
    private String nsisSmart;

    @Column
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_uuid")
    private User responsibleUser;

    @Column
    @Builder.Default
    private boolean selected = true;

    @Column
    @Enumerated(EnumType.STRING)
    private StandardSectionStatus status;

    @Override
    public RelationType getRelationType() {
        return RelationType.STANDARD_SECTION;
    }

    @Override
    public String getLocalizedEnumValues() {
        return status != null ? status.getMessage() : "";
    }
}
