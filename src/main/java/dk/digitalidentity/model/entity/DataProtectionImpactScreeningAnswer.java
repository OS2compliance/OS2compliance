package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "dpia_screening_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataProtectionImpactScreeningAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private DataProtectionImpactAssessment assessment;

    // Choice answers from the DPIA screening
    @OneToOne
    @JoinColumn(name = "choice_id")
    private ChoiceDPIA choice;

    @Column
    private String answer;

}
