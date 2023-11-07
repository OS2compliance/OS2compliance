package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "threat_catalog_threats")
@Getter
@Setter
public class ThreatCatalogThreat {
    @Id
    @Column(nullable = false, unique = true)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_catalog_identifier")
    private ThreatCatalog threatCatalog;

    @Column
    private String threatType;

    @Column
    private String description;

    @Column
    private boolean rights;

}
