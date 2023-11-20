package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "threat_catalogs")
@Getter
@Setter
public class ThreatCatalog {
    @Id
    @Column(nullable = false, unique = true)
    private String identifier;

    @OneToMany(mappedBy = "threatCatalog")
    private List<ThreatAssessment> assessments;

    @OneToMany(mappedBy = "threatCatalog")
    private List<ThreatCatalogThreat> threats;

    @Column
    private String name;
}
