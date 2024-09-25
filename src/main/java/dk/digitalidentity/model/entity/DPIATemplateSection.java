package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dpia_template_section")
@Getter
@Setter
public class DPIATemplateSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private Long sortKey;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String heading;

    @Column
    private String explainer;

    @Column
    private boolean canOptOut;

    @Column
    private boolean hasOptedOut;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "dpiaTemplateSection", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DPIATemplateQuestion> dpiaTemplateQuestions = new ArrayList<>();
}
