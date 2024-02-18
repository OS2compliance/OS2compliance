package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;

@Entity
@Table(name = "threat_catalogs")
@Getter
@Setter
@SQLDelete(sql = "UPDATE threat_catalogs SET deleted = true WHERE id=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class ThreatCatalog {
    @Id
    @Column(nullable = false, unique = true)
    private String identifier;

    @OneToMany(mappedBy = "threatCatalog")
    @JsonIgnore
    private List<ThreatAssessment> assessments;

    @OneToMany(mappedBy = "threatCatalog")
    @OrderBy(value = "sortKey ASC")
    private List<ThreatCatalogThreat> threats;

    @Column
    private String name;

    @Column
    private boolean hidden = true;
}
