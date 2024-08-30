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
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "dpia_template_question")
@Getter
@Setter
@SQLDelete(sql = "UPDATE threat_catalogs SET deleted = true WHERE identifier=?", check = ResultCheckStyle.COUNT)
@Where(clause = "deleted=false")
public class DPIATemplateQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "dpia_template_section_id")
    private DPIATemplateSection dpiaTemplateSection;

    @Column
    private Long sortKey;

    @Column(nullable = false)
    private String question;

    @Column
    private String instructions;

    @Column
    private String answerTemplate;

    @Column
    private boolean deleted;
}
