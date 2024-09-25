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
@Table(name = "dpia_response_section")
@Getter
@Setter
public class DPIAResponseSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "dpia_id")
    private DPIA dpia;

    @ManyToOne
    @JoinColumn(name = "dpia_template_section_id")
    private DPIATemplateSection dpiaTemplateSection;

    @Column
    private boolean selected;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(orphanRemoval = true, mappedBy = "dpiaResponseSection", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DPIAResponseSectionAnswer> dpiaResponseSectionAnswers = new ArrayList<>();
}
