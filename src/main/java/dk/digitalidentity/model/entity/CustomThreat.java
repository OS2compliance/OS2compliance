package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "custom_threats")
@Getter
@Setter
public class CustomThreat {
    /**
     * Draws from the same {@code default} segment as the whole {@link Relatable} hierarchy. Giving it
     * its own segment would restart allocation at 1 and collide with the rows already in
     * custom_threats.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = Relatable.ID_GENERATOR)
    private Long id;

    @Column
    private String threatType;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threat_assessment_id")
    private ThreatAssessment threatAssessment;

}
