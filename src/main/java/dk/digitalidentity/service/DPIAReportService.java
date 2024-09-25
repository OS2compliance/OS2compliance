package dk.digitalidentity.service;

import dk.digitalidentity.dao.DPIAReportDao;
import dk.digitalidentity.model.entity.DPIAReport;
import dk.digitalidentity.model.entity.S3Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DPIAReportService {
    private final DPIAReportDao dpiaReportDao;
    public DPIAReport findByS3Document(S3Document s3Document) {
        return dpiaReportDao.findByDpiaReportS3DocumentId(s3Document.getId());
    }

    public void save(DPIAReport dpiaReport) {
        dpiaReportDao.save(dpiaReport);
    }
}
