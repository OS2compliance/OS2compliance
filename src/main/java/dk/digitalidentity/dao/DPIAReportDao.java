package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.DPIAReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DPIAReportDao extends JpaRepository<DPIAReport, Long> {

    DPIAReport findByDpiaReportS3DocumentId(long id);

}
