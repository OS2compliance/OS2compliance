package dk.digitalidentity.integration.dbs;

import dk.dbs.api.DocumentResourceApi;
import dk.dbs.api.ItSystemsResourceApi;
import dk.dbs.api.SupplierResourceApi;
import dk.dbs.api.model.Document;
import dk.dbs.api.model.ItSystem;
import dk.dbs.api.model.PageEODocument;
import dk.dbs.api.model.PageEOItSystem;
import dk.dbs.api.model.PageEOSupplier;
import dk.dbs.api.model.Supplier;
import dk.digitalidentity.integration.dbs.exception.DBSSynchronizationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class DBSClientService {

    private final ItSystemsResourceApi itSystemResourceApi;
    private final SupplierResourceApi supplierResourceApi;
    private final DocumentResourceApi documentResourceApi;

    @Transactional(Transactional.TxType.NEVER)
    public List<Supplier> getAllSuppliers() {
        int page = 0;

        PageEOSupplier supplierPage = supplierResourceApi.callList(100, page);
        if (supplierPage == null || supplierPage.getContent() == null || supplierPage.getContent().isEmpty()) {
            throw new DBSSynchronizationException("Could not fetch Suppliers from DBS");
        }
        List<Supplier> suppliers = new ArrayList<>(supplierPage.getContent());
        while (supplierPage != null && supplierPage.getTotalPages() != null && page < supplierPage.getTotalPages()) {
            page += 1;
            supplierPage = supplierResourceApi.callList(100, page);
            if (supplierPage != null && supplierPage.getContent() != null) {
                suppliers.addAll(supplierPage.getContent());
            }
        }

        return suppliers;
    }

    @Transactional(Transactional.TxType.NEVER)
    public List<ItSystem> getAllItSystems() {
        int page = 0;

        PageEOItSystem itSystemsPage = itSystemResourceApi.list1(100, page);
        if (itSystemsPage == null || itSystemsPage.getContent() == null || itSystemsPage.getContent().isEmpty()) {
            throw new DBSSynchronizationException("Could not fetch ItSystems from DBS");
        }

        final List<ItSystem> itSystems = new ArrayList<>(itSystemsPage.getContent());
        while (itSystemsPage != null && itSystemsPage.getTotalPages() != null && page < itSystemsPage.getTotalPages()) {
            page += 1;
            itSystemsPage = itSystemResourceApi.list1(100, page);
            if (itSystemsPage != null && itSystemsPage.getContent() != null) {
                itSystems.addAll(itSystemsPage.getContent());
            }
        }

        return itSystems;
    }

    @Transactional(Transactional.TxType.NEVER)
    public List<Document> getAllDocuments(LocalDateTime createdAfter) {
        int page = 0;

        PageEODocument documentsPage = documentResourceApi.list2(100, page, "TILSYNSRAPPORTER", createdAfter);
        if (documentsPage == null || documentsPage.getContent() == null) {
            throw new DBSSynchronizationException("Could not fetch ItSystems from DBS");
        }

        List<Document> documents = new ArrayList<>(documentsPage.getContent());

        while (documentsPage != null && documentsPage.getTotalPages() != null && page < documentsPage.getTotalPages()) {
            page += 1;
            documentsPage = documentResourceApi.list2(100, page, "TILSYNSRAPPORTER", createdAfter);
            if (documentsPage != null && documentsPage.getContent() != null) {
                documents.addAll(documentsPage.getContent());
            }
        }
        return documents;
    }

}
