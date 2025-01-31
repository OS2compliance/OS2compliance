package dk.digitalidentity.report;

import dk.digitalidentity.model.entity.Incident;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class IncidentsXlsView extends AbstractXlsView {
    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final Sheet sheet = workbook.createSheet("Årshjul " + LocalDate.now().getYear() + "+");
        final Page<Incident> allIncidents = (Page<Incident>) model.get("incidents");
        final LocalDateTime fromDT = (LocalDateTime) model.get("from");
        final LocalDateTime toDT = (LocalDateTime) model.get("to");
        // TODO
    }

    private List<String> findColumnNames(final Incident incident) {

    }
}
