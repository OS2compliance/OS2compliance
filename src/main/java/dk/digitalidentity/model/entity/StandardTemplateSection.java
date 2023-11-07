package dk.digitalidentity.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "standard_template_sections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardTemplateSection {
    @Id
    @Column(nullable = false, unique = true)
    private String identifier;

    @Column
    private String section;

    @Column
    private String description;

    @Column
    private String securityLevel;

    @Column
    private int sortKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_identifier")
    private StandardTemplateSection parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<StandardTemplateSection> children = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_template_identifier")
    private StandardTemplate standardTemplate;

    @OneToOne(mappedBy = "templateSection", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private StandardSection standardSection;
}
