package dk.digitalidentity.report.riskimage;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.report.XlsUtil;
import dk.digitalidentity.report.systemowneroverview.dto.TaskRow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dk.digitalidentity.report.XlsUtil.*;

public class RiskImageView extends AbstractXlsView {
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {

		Set<ThreatAssessment> threatAssessments = setFromModel(model, "threatAssessments");
		createRiskImageSheet(workbook, threatAssessments);

	}

	private void createRiskImageSheet(Workbook workbook, Set<ThreatAssessment> threatAssessments) {
		String sheetName = "Risikobillede";

		Map<String, XlsUtil.CellValueSetter<ThreatAssessment, Cell>> converterMap = new LinkedHashMap<>();
		converterMap.put("id", (t, c) -> c.setCellValue(t.getId()));

		createSheet(sheetName, threatAssessments.stream().toList(), converterMap, workbook);

	}

	private <T> Set<T> setFromModel(Map<String, Object> model, String propertyName) {
		try {
			Object modelObject = model.get(propertyName);
			if (modelObject instanceof Set<?> rawSet && !rawSet.isEmpty() && rawSet.iterator().next() instanceof TaskRow) {
					@SuppressWarnings("unchecked")
					Set<T> set = (Set<T>) rawSet;
					return set;
				}
			return Collections.emptySet();
		}
		catch (Exception e) {
			return Collections.emptySet();
		}
	}

}
