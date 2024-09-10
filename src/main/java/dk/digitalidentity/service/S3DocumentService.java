package dk.digitalidentity.service;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.dao.S3DocumentDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessment;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.TransferImpactAssessment;
import dk.digitalidentity.model.entity.enums.RelationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class S3DocumentService {
    private final S3DocumentDao s3DocumentDao;

    public Optional<S3Document> get(final Long id) {
        return s3DocumentDao.findById(id);
    }

    public S3Document save(S3Document s3Document) {
        return s3DocumentDao.save(s3Document);
    }
}
