package dk.digitalidentity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dpia_response_section_answer")
@Getter
@Setter
public class DPIAResponseSectionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "dpia_response_section_id")
    private DPIAResponseSection dpiaResponseSection;

    @ManyToOne
    @JoinColumn(name = "dpia_template_question_id")
    private DPIATemplateQuestion dpiaTemplateQuestion;

    @Column
    private String response;
}
